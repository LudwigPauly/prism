//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <dxp@cs.bham.uc.uk> (University of Birmingham)
//	
//------------------------------------------------------------------------------
//	
//	This file is part of PRISM.
//	
//	PRISM is free software; you can redistribute it and/or modify
//	it under the terms of the GNU General Public License as published by
//	the Free Software Foundation; either version 2 of the License, or
//	(at your option) any later version.
//	
//	PRISM is distributed in the hope that it will be useful,
//	but WITHOUT ANY WARRANTY; without even the implied warranty of
//	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//	GNU General Public License for more details.
//	
//	You should have received a copy of the GNU General Public License
//	along with PRISM; if not, write to the Free Software Foundation,
//	Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//	
//==============================================================================

// includes
#include "PrismSparse.h"
#include <math.h>
#include <util.h>
#include <cudd.h>
#include <dd.h>
#include <odd.h>
#include <dv.h>
#include "sparse.h"
#include "PrismSparseGlob.h"
#include "jnipointer.h"

//------------------------------------------------------------------------------

JNIEXPORT jlong __pointer JNICALL Java_sparse_PrismSparse_PS_1ProbCumulReward
(
JNIEnv *env,
jclass cls,
jlong __pointer t,	// trans matrix
jlong __pointer sr,	// state rewards
jlong __pointer trr,// transition rewards
jlong __pointer od,	// odd
jlong __pointer rv,	// row vars
jint num_rvars,
jlong __pointer cv,	// col vars
jint num_cvars,
jint bound			// time bound
)
{
	// cast function parameters
	DdNode *trans = jlong_to_DdNode(t);		// trans matrix
	DdNode *state_rewards = jlong_to_DdNode(sr);	// state rewards
	DdNode *trans_rewards = jlong_to_DdNode(trr);	// transition rewards
	ODDNode *odd = jlong_to_ODDNode(od);		// reachable states
	DdNode **rvars = jlong_to_DdNode_array(rv);	// row vars
	DdNode **cvars = jlong_to_DdNode_array(cv);	// col vars

	// model stats
	int n;
	long nnz;
	// flags
	bool compact_tr, compact_r;
	// sparse matrix
	RMSparseMatrix *rmsm;
	CMSRSparseMatrix *cmsrsm;
	// vectors
	double *rew_vec, *soln, *soln2, *tmpsoln;
	DistVector *rew_dist;
	// timing stuff
	long start1, start2, start3, stop;
	double time_taken, time_for_setup, time_for_iters;
	// misc
	int i, j, l, h, iters;
	double d, kb, kbt;
	
	// start clocks	
	start1 = start2 = util_cpu_time();
	
	// get number of states
	n = odd->eoff + odd->toff;
	
	// build sparse matrix
	PS_PrintToMainLog(env, "\nBuilding sparse matrix... ");
	// if requested, try and build a "compact" version
	compact_tr = true;
	cmsrsm = NULL;
	if (compact) cmsrsm = build_cmsr_sparse_matrix(ddman, trans, rvars, cvars, num_rvars, odd);
	if (cmsrsm != NULL) {
		nnz = cmsrsm->nnz;
		kb = cmsrsm->mem;
	}
	// if not or if it wasn't possible, built a normal one
	else {
		compact_tr = false;
		rmsm = build_rm_sparse_matrix(ddman, trans, rvars, cvars, num_rvars, odd);
		nnz = rmsm->nnz;
		kb = rmsm->mem;
	}
	// print some info
	PS_PrintToMainLog(env, "[n=%d, nnz=%d%s] ", n, nnz, compact_tr?", compact":"");
	kbt = kb;
	PS_PrintToMainLog(env, "[%.1f KB]\n", kb);
	
	// multiply transition rewards by transition probs and sum rows
	// (note also filters out unwanted states at the same time)
	Cudd_Ref(trans);
	trans_rewards = DD_Apply(ddman, APPLY_TIMES, trans_rewards, trans);
	trans_rewards = DD_SumAbstract(ddman, trans_rewards, cvars, num_cvars);
	
	// combine state and transition rewards and put in a vector
	Cudd_Ref(trans_rewards);
	state_rewards = DD_Apply(ddman, APPLY_PLUS, state_rewards, trans_rewards);
	
	// get vector of rewards
	PS_PrintToMainLog(env, "Creating vector for rewards... ");
	rew_vec = mtbdd_to_double_vector(ddman, state_rewards, rvars, num_rvars, odd);
	// try and convert to compact form if required
	compact_r = false;
	if (compact) {
		if (rew_dist = double_vector_to_dist(rew_vec, n)) {
			compact_r = true;
			free(rew_vec);
		}
	}
	kb = (!compact_r) ? n*8.0/1024.0 : (rew_dist->num_dist*8.0+n*2.0)/1024.0;
	kbt += kb;
	if (!compact_r) PS_PrintToMainLog(env, "[%.1f KB]\n", kb);
	else PS_PrintToMainLog(env, "[dist=%d, compact] [%.1f KB]\n", rew_dist->num_dist, kb);
	
	// create solution/iteration vectors
	PS_PrintToMainLog(env, "Allocating iteration vectors... ");
	soln = new double[n];
	soln2 = new double[n];
	kb = n*8.0/1024.0;
	kbt += 2*kb;
	PS_PrintToMainLog(env, "[2 x %.1f KB]\n", kb);
	
	// print total memory usage
	PS_PrintToMainLog(env, "TOTAL: [%.1f KB]\n", kbt);
	
	// initial solution is zero
	for (i = 0; i < n; i++) {
		soln[i] = 0;
	}
	
	// get setup time
	stop = util_cpu_time();
	time_for_setup = (double)(stop - start2)/1000;
	start2 = stop;
	
	// start iterations
	PS_PrintToMainLog(env, "\nStarting iterations...\n");
	
	// note that we ignore max_iters as we know how any iterations _should_ be performed
	for (iters = 0; iters < bound; iters++) {
		
//		PS_PrintToMainLog(env, "Iteration %d: ", iters);
//		start3 = util_cpu_time();
		
		// store local copies of stuff
		double *non_zeros;
		unsigned char *row_counts;
		int *row_starts;
		bool use_counts;
		unsigned int *cols;
		double *dist;
		int dist_shift;
		int dist_mask;
		if (!compact_tr) {
			non_zeros = rmsm->non_zeros;
			row_counts = rmsm->row_counts;
			row_starts = (int *)rmsm->row_counts;
			use_counts = rmsm->use_counts;
			cols = rmsm->cols;
		} else {
			row_counts = cmsrsm->row_counts;
			row_starts = (int *)cmsrsm->row_counts;
			use_counts = cmsrsm->use_counts;
			cols = cmsrsm->cols;
			dist = cmsrsm->dist;
			dist_shift = cmsrsm->dist_shift;
			dist_mask = cmsrsm->dist_mask;
		}
		
		// matrix multiply
		h = 0;
		for (i = 0; i < n; i++) {
			d = (!compact_r) ? rew_vec[i] : rew_dist->dist[rew_dist->ptrs[i]];
			if (!use_counts) { l = row_starts[i]; h = row_starts[i+1]; }
			else { l = h; h += row_counts[i]; }
			// "row major" version
			if (!compact_tr) {
				for (j = l; j < h; j++) {
					d += non_zeros[j] * soln[cols[j]];
				}
			// "compact msr" version
			} else {
				for (j = l; j < h; j++) {
					d += dist[(int)(cols[j] & dist_mask)] * soln[(int)(cols[j] >> dist_shift)];
				}
			}
			// set vector element
			soln2[i] = d;
		}
		
		// prepare for next iteration
		tmpsoln = soln;
		soln = soln2;
		soln2 = tmpsoln;
		
//		PS_PrintToMainLog(env, "%.2f %.2f sec\n", ((double)(util_cpu_time() - start3)/1000), ((double)(util_cpu_time() - start2)/1000)/iters);
	}
	
	// stop clocks
	stop = util_cpu_time();
	time_for_iters = (double)(stop - start2)/1000;
	time_taken = (double)(stop - start1)/1000;
	
	// print iterations/timing info
	PS_PrintToMainLog(env, "\nIterative method: %d iterations in %.2f seconds (average %.6f, setup %.2f)\n", iters, time_taken, time_for_iters/iters, time_for_setup);
	
	// free memory
	if (compact_tr) free_cmsr_sparse_matrix(cmsrsm); else free_rm_sparse_matrix(rmsm);
	if (compact_r) free_dist_vector(rew_dist); else free(rew_vec);
	delete soln2;
	
	return ptr_to_jlong(soln);
}

//------------------------------------------------------------------------------
