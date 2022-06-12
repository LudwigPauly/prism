package param;

import prism.PrismException;

/**
 * class for solving system of linear equations with gauss elemination for functions
 */
public class GaussElemination
{
	private Function[][] l;    // L
	private Function[][] u;    // U

	private Function[] y;    // solution vector

	private FunctionFactory functionFactory;

	private int n;    // rank of the matrix

	private boolean decomposed = false;

	/**
	 * Method to execute a gauss elemination for solving a system of linear equations Ax = b
	 * @param a Matrix A
	 * @param b Vector b
	 * @param factory function factory
	 * @return solution vector
	 * @throws PrismException thrown if matrix a or vector b not in the right format
	 */
	public Function[] solve(Function[][] a, Function[] b, FunctionFactory factory) throws PrismException
	{
		// some checks
		if (a.length != a[0].length)
			throw new PrismException("Matrix A must be a square matrix!");
		if (a.length != b.length)
			throw new PrismException("Rank of matrix A doesn't match with length of Vector b!");

		int n = b.length;

		this.functionFactory = factory;

		Function tmp;

		// colum iteration (n-1)
		for (int i = 0; i < n - 1; i++) {
			// row iteration
			for (int k = i + 1; k < n; k++) {
				// check for zero
				if (a[i][i].isZero()) {
					throw new PrismException("Cannot devide by Zero");
				}
				//compute l
				//l[k][i] = u[k][i] - u[i][i]
				tmp = a[k][i].divide(a[i][i]);

				//iteration over colums >= i
				//compute a
				for (int j = i; j < n; j++) {
					//u[k][j] = u[k][j] - tmp * u[i][j]
					a[k][j] = a[k][j].subtract(tmp.multiply(a[i][j]));
				}
				b[k] = b[k].subtract(tmp.multiply(b[i]));
			}
		}

		for (int i = n - 1; i >= 0; i--) {
			tmp = functionFactory.getZero();
			for (int k = i + 1; k < n; k++) {
				tmp = tmp.add(a[i][k].multiply(b[k]));
			}

			tmp = b[i].subtract(tmp);
			if (a[i][i].isZero()) {
				throw new PrismException("Cannot devide by Zero");
			}
			b[i] = tmp.multiply(functionFactory.getOne().divide(a[i][i]));
		}

		return b;
	}

	public void decompesition(Function[][] a,FunctionFactory factory) throws PrismException
	{
		if (a.length != a[0].length)
			throw new PrismException("Matrix A must be a square matrix!");

		this.functionFactory = factory;
		this.n = a.length;

		this.l = new Function[n][n];
		this.u = new Function[n][n];

		// init L
		// L = I
		for (int k = 0; k < n; k++) {
			for (int i = 0; i < n; i++) {
				if (i == k)
					l[k][i] = functionFactory.getOne();
				else
					l[k][i] = functionFactory.getZero();
			}
		}

		// init U
		// U = A
		this.u = a;

		luDecomposition();
		decomposed = true;
	}

	public Function [] solveLU(Function[] b) throws PrismException
	{
		// some checks
		if(!decomposed)
			throw new PrismException("Matrix A not decomposed!");

		if (n != b.length)
			throw new PrismException("Rank of matrix A doesn't match with length of Vector b!");

		this.y = b;

		forwardSubstituation();
		backwardSubstituation();

		return y;
	}



	/**
	 * lu decomposition according to gauss elemination
	 * @throws PrismException when devided by zero
	 */
	protected void luDecomposition() throws PrismException
	{
		// colum iteration (n-1)
		for (int i = 0; i < n - 1; i++) {
			// row iteration
			for (int k = i + 1; k < n; k++) {
				// check for zero
				if (u[i][i].isZero()) {
					throw new PrismException("Cannot devide by Zero");
				}
				//compute l
				//l[k][i] = u[k][i] - u[i][i]
				l[k][i] = u[k][i].divide(u[i][i]);

				//iteration over colums >= i
				//compute u
				for (int j = i; j < n; j++) {
					//u[k][j] = u[k][j] - l[k][i] * u[i][j]
					u[k][j] = u[k][j].subtract(l[k][i].multiply(u[i][j]));
				}
			}
		}
	}

	/**
	 * forward substituation according to gauss elemination
	 * @throws PrismException when devided by zero
	 */
	protected void forwardSubstituation() throws PrismException
	{
		Function tmp;

		for (int i = 0; i < n; i++) {
			tmp = functionFactory.getZero();
			for (int k = 0; k < i; k++) {
				tmp = tmp.add(l[i][k].multiply(y[k]));
			}
			tmp = y[i].subtract(tmp);
			if (l[i][i].isZero()) {
				throw new PrismException("Cannot devide by Zero");
			}
			y[i] = tmp.multiply(functionFactory.getOne().divide(l[i][i]));
		}
	}

	/**
	 * backwards substituation according to gauss elemination
	 * @throws PrismException when devided by zero
	 */
	protected void backwardSubstituation() throws PrismException
	{
		Function tmp;

		for (int i = n - 1; i >= 0; i--) {
			tmp = functionFactory.getZero();
			for (int k = i + 1; k < n; k++) {
				tmp = tmp.add(u[i][k].multiply(y[k]));
			}
			tmp = y[i].subtract(tmp);
			if (u[i][i].isZero()) {
				throw new PrismException("Cannot devide by Zero");
			}
			y[i] = tmp.multiply(functionFactory.getOne().divide(u[i][i]));
		}
	}

}
