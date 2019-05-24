/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class mtbdd_PrismMTBDD */

#ifndef _Included_mtbdd_PrismMTBDD
#define _Included_mtbdd_PrismMTBDD
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     mtbdd_PrismMTBDD
 * Method:    PM_FreeGlobalRefs
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_mtbdd_PrismMTBDD_PM_1FreeGlobalRefs
  (JNIEnv *, jclass);

/*
 * Class:     mtbdd_PrismMTBDD
 * Method:    PM_SetCUDDManager
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_mtbdd_PrismMTBDD_PM_1SetCUDDManager
  (JNIEnv *, jclass, jlong);

/*
 * Class:     mtbdd_PrismMTBDD
 * Method:    PM_SetMainLog
 * Signature: (Lprism/PrismLog;)V
 */
JNIEXPORT void JNICALL Java_mtbdd_PrismMTBDD_PM_1SetMainLog
  (JNIEnv *, jclass, jobject);

/*
 * Class:     mtbdd_PrismMTBDD
 * Method:    PM_SetTechLog
 * Signature: (Lprism/PrismLog;)V
 */
JNIEXPORT void JNICALL Java_mtbdd_PrismMTBDD_PM_1SetTechLog
  (JNIEnv *, jclass, jobject);

/*
 * Class:     mtbdd_PrismMTBDD
 * Method:    PM_SetExportIterations
 * Signature: (Z)V
 */
JNIEXPORT void JNICALL Java_mtbdd_PrismMTBDD_PM_1SetExportIterations
  (JNIEnv *, jclass, jboolean);

/*
 * Class:     mtbdd_PrismMTBDD
 * Method:    PM_GetErrorMessage
 * Signature: ()Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_mtbdd_PrismMTBDD_PM_1GetErrorMessage
  (JNIEnv *, jclass);

/*
 * Class:     mtbdd_PrismMTBDD
 * Method:    PM_Reachability
 * Signature: (JJIJIJ)J
 */
JNIEXPORT jlong JNICALL Java_mtbdd_PrismMTBDD_PM_1Reachability
  (JNIEnv *, jclass, jlong, jlong, jint, jlong, jint, jlong);

/*
 * Class:     mtbdd_PrismMTBDD
 * Method:    PM_Prob1
 * Signature: (JJJIJIJJJ)J
 */
JNIEXPORT jlong JNICALL Java_mtbdd_PrismMTBDD_PM_1Prob1
  (JNIEnv *, jclass, jlong, jlong, jlong, jint, jlong, jint, jlong, jlong, jlong);

/*
 * Class:     mtbdd_PrismMTBDD
 * Method:    PM_Prob0
 * Signature: (JJJIJIJJ)J
 */
JNIEXPORT jlong JNICALL Java_mtbdd_PrismMTBDD_PM_1Prob0
  (JNIEnv *, jclass, jlong, jlong, jlong, jint, jlong, jint, jlong, jlong);

/*
 * Class:     mtbdd_PrismMTBDD
 * Method:    PM_Prob1E
 * Signature: (JJJIJIJIJJJ)J
 */
JNIEXPORT jlong JNICALL Java_mtbdd_PrismMTBDD_PM_1Prob1E
  (JNIEnv *, jclass, jlong, jlong, jlong, jint, jlong, jint, jlong, jint, jlong, jlong, jlong);

/*
 * Class:     mtbdd_PrismMTBDD
 * Method:    PM_Prob1A
 * Signature: (JJJJIJIJIJJ)J
 */
JNIEXPORT jlong JNICALL Java_mtbdd_PrismMTBDD_PM_1Prob1A
  (JNIEnv *, jclass, jlong, jlong, jlong, jlong, jint, jlong, jint, jlong, jint, jlong, jlong);

/*
 * Class:     mtbdd_PrismMTBDD
 * Method:    PM_Prob0E
 * Signature: (JJJJIJIJIJJ)J
 */
JNIEXPORT jlong JNICALL Java_mtbdd_PrismMTBDD_PM_1Prob0E
  (JNIEnv *, jclass, jlong, jlong, jlong, jlong, jint, jlong, jint, jlong, jint, jlong, jlong);

/*
 * Class:     mtbdd_PrismMTBDD
 * Method:    PM_Prob0A
 * Signature: (JJJIJIJIJJ)J
 */
JNIEXPORT jlong JNICALL Java_mtbdd_PrismMTBDD_PM_1Prob0A
  (JNIEnv *, jclass, jlong, jlong, jlong, jint, jlong, jint, jlong, jint, jlong, jlong);

/*
 * Class:     mtbdd_PrismMTBDD
 * Method:    PM_ProbBoundedUntil
 * Signature: (JJJIJIJJI)J
 */
JNIEXPORT jlong JNICALL Java_mtbdd_PrismMTBDD_PM_1ProbBoundedUntil
  (JNIEnv *, jclass, jlong, jlong, jlong, jint, jlong, jint, jlong, jlong, jint);

/*
 * Class:     mtbdd_PrismMTBDD
 * Method:    PM_ProbUntil
 * Signature: (JJJIJIJJ)J
 */
JNIEXPORT jlong JNICALL Java_mtbdd_PrismMTBDD_PM_1ProbUntil
  (JNIEnv *, jclass, jlong, jlong, jlong, jint, jlong, jint, jlong, jlong);

/*
 * Class:     mtbdd_PrismMTBDD
 * Method:    PM_ProbUntilInterval
 * Signature: (JJJIJIJJI)J
 */
JNIEXPORT jlong JNICALL Java_mtbdd_PrismMTBDD_PM_1ProbUntilInterval
  (JNIEnv *, jclass, jlong, jlong, jlong, jint, jlong, jint, jlong, jlong, jint);

/*
 * Class:     mtbdd_PrismMTBDD
 * Method:    PM_ProbCumulReward
 * Signature: (JJJJJIJII)J
 */
JNIEXPORT jlong JNICALL Java_mtbdd_PrismMTBDD_PM_1ProbCumulReward
  (JNIEnv *, jclass, jlong, jlong, jlong, jlong, jlong, jint, jlong, jint, jint);

/*
 * Class:     mtbdd_PrismMTBDD
 * Method:    PM_ProbInstReward
 * Signature: (JJJJIJII)J
 */
JNIEXPORT jlong JNICALL Java_mtbdd_PrismMTBDD_PM_1ProbInstReward
  (JNIEnv *, jclass, jlong, jlong, jlong, jlong, jint, jlong, jint, jint);

/*
 * Class:     mtbdd_PrismMTBDD
 * Method:    PM_ProbReachReward
 * Signature: (JJJJJIJIJJJ)J
 */
JNIEXPORT jlong JNICALL Java_mtbdd_PrismMTBDD_PM_1ProbReachReward
  (JNIEnv *, jclass, jlong, jlong, jlong, jlong, jlong, jint, jlong, jint, jlong, jlong, jlong);

/*
 * Class:     mtbdd_PrismMTBDD
 * Method:    PM_ProbReachRewardInterval
 * Signature: (JJJJJIJIJJJJJI)J
 */
JNIEXPORT jlong JNICALL Java_mtbdd_PrismMTBDD_PM_1ProbReachRewardInterval
  (JNIEnv *, jclass, jlong, jlong, jlong, jlong, jlong, jint, jlong, jint, jlong, jlong, jlong, jlong, jlong, jint);

/*
 * Class:     mtbdd_PrismMTBDD
 * Method:    PM_ProbTransient
 * Signature: (JJJJIJII)J
 */
JNIEXPORT jlong JNICALL Java_mtbdd_PrismMTBDD_PM_1ProbTransient
  (JNIEnv *, jclass, jlong, jlong, jlong, jlong, jint, jlong, jint, jint);

/*
 * Class:     mtbdd_PrismMTBDD
 * Method:    PM_NondetBoundedUntil
 * Signature: (JJJJIJIJIJJIZ)J
 */
JNIEXPORT jlong JNICALL Java_mtbdd_PrismMTBDD_PM_1NondetBoundedUntil
  (JNIEnv *, jclass, jlong, jlong, jlong, jlong, jint, jlong, jint, jlong, jint, jlong, jlong, jint, jboolean);

/*
 * Class:     mtbdd_PrismMTBDD
 * Method:    PM_NondetUntil
 * Signature: (JJJJIJIJIJJZ)J
 */
JNIEXPORT jlong JNICALL Java_mtbdd_PrismMTBDD_PM_1NondetUntil
  (JNIEnv *, jclass, jlong, jlong, jlong, jlong, jint, jlong, jint, jlong, jint, jlong, jlong, jboolean);

/*
 * Class:     mtbdd_PrismMTBDD
 * Method:    PM_NondetUntilInterval
 * Signature: (JJJJIJIJIJJZI)J
 */
JNIEXPORT jlong JNICALL Java_mtbdd_PrismMTBDD_PM_1NondetUntilInterval
  (JNIEnv *, jclass, jlong, jlong, jlong, jlong, jint, jlong, jint, jlong, jint, jlong, jlong, jboolean, jint);

/*
 * Class:     mtbdd_PrismMTBDD
 * Method:    PM_NondetInstReward
 * Signature: (JJJJJIJIJIIZJ)J
 */
JNIEXPORT jlong JNICALL Java_mtbdd_PrismMTBDD_PM_1NondetInstReward
  (JNIEnv *, jclass, jlong, jlong, jlong, jlong, jlong, jint, jlong, jint, jlong, jint, jint, jboolean, jlong);

/*
 * Class:     mtbdd_PrismMTBDD
 * Method:    PM_NondetReachReward
 * Signature: (JJJJJJIJIJIJJJZ)J
 */
JNIEXPORT jlong JNICALL Java_mtbdd_PrismMTBDD_PM_1NondetReachReward
  (JNIEnv *, jclass, jlong, jlong, jlong, jlong, jlong, jlong, jint, jlong, jint, jlong, jint, jlong, jlong, jlong, jboolean);

/*
 * Class:     mtbdd_PrismMTBDD
 * Method:    PM_NondetReachRewardInterval
 * Signature: (JJJJJJIJIJIJJJJJZI)J
 */
JNIEXPORT jlong JNICALL Java_mtbdd_PrismMTBDD_PM_1NondetReachRewardInterval
  (JNIEnv *, jclass, jlong, jlong, jlong, jlong, jlong, jlong, jint, jlong, jint, jlong, jint, jlong, jlong, jlong, jlong, jlong, jboolean, jint);

/*
 * Class:     mtbdd_PrismMTBDD
 * Method:    PM_StochBoundedUntil
 * Signature: (JJJIJIJJDJ)J
 */
JNIEXPORT jlong JNICALL Java_mtbdd_PrismMTBDD_PM_1StochBoundedUntil
  (JNIEnv *, jclass, jlong, jlong, jlong, jint, jlong, jint, jlong, jlong, jdouble, jlong);

/*
 * Class:     mtbdd_PrismMTBDD
 * Method:    PM_StochCumulReward
 * Signature: (JJJJJIJID)J
 */
JNIEXPORT jlong JNICALL Java_mtbdd_PrismMTBDD_PM_1StochCumulReward
  (JNIEnv *, jclass, jlong, jlong, jlong, jlong, jlong, jint, jlong, jint, jdouble);

/*
 * Class:     mtbdd_PrismMTBDD
 * Method:    PM_StochSteadyState
 * Signature: (JJJJIJI)J
 */
JNIEXPORT jlong JNICALL Java_mtbdd_PrismMTBDD_PM_1StochSteadyState
  (JNIEnv *, jclass, jlong, jlong, jlong, jlong, jint, jlong, jint);

/*
 * Class:     mtbdd_PrismMTBDD
 * Method:    PM_StochTransient
 * Signature: (JJJJIJID)J
 */
JNIEXPORT jlong JNICALL Java_mtbdd_PrismMTBDD_PM_1StochTransient
  (JNIEnv *, jclass, jlong, jlong, jlong, jlong, jint, jlong, jint, jdouble);

/*
 * Class:     mtbdd_PrismMTBDD
 * Method:    PM_ExportVector
 * Signature: (JLjava/lang/String;JIJILjava/lang/String;)I
 */
JNIEXPORT jint JNICALL Java_mtbdd_PrismMTBDD_PM_1ExportVector
  (JNIEnv *, jclass, jlong, jstring, jlong, jint, jlong, jint, jstring);

/*
 * Class:     mtbdd_PrismMTBDD
 * Method:    PM_ExportMatrix
 * Signature: (JLjava/lang/String;JIJIJILjava/lang/String;)I
 */
JNIEXPORT jint JNICALL Java_mtbdd_PrismMTBDD_PM_1ExportMatrix
  (JNIEnv *, jclass, jlong, jstring, jlong, jint, jlong, jint, jlong, jint, jstring);

/*
 * Class:     mtbdd_PrismMTBDD
 * Method:    PM_ExportLabels
 * Signature: ([J[Ljava/lang/String;Ljava/lang/String;JIJILjava/lang/String;)I
 */
JNIEXPORT jint JNICALL Java_mtbdd_PrismMTBDD_PM_1ExportLabels
  (JNIEnv *, jclass, jlongArray, jobjectArray, jstring, jlong, jint, jlong, jint, jstring);

/*
 * Class:     mtbdd_PrismMTBDD
 * Method:    PM_Power
 * Signature: (JJIJIJJJZ)J
 */
JNIEXPORT jlong JNICALL Java_mtbdd_PrismMTBDD_PM_1Power
  (JNIEnv *, jclass, jlong, jlong, jint, jlong, jint, jlong, jlong, jlong, jboolean);

/*
 * Class:     mtbdd_PrismMTBDD
 * Method:    PM_PowerInterval
 * Signature: (JJIJIJJJJZI)J
 */
JNIEXPORT jlong JNICALL Java_mtbdd_PrismMTBDD_PM_1PowerInterval
  (JNIEnv *, jclass, jlong, jlong, jint, jlong, jint, jlong, jlong, jlong, jlong, jboolean, jint);

/*
 * Class:     mtbdd_PrismMTBDD
 * Method:    PM_JOR
 * Signature: (JJIJIJJJZD)J
 */
JNIEXPORT jlong JNICALL Java_mtbdd_PrismMTBDD_PM_1JOR
  (JNIEnv *, jclass, jlong, jlong, jint, jlong, jint, jlong, jlong, jlong, jboolean, jdouble);

/*
 * Class:     mtbdd_PrismMTBDD
 * Method:    PM_JORInterval
 * Signature: (JJIJIJJJJZDI)J
 */
JNIEXPORT jlong JNICALL Java_mtbdd_PrismMTBDD_PM_1JORInterval
  (JNIEnv *, jclass, jlong, jlong, jint, jlong, jint, jlong, jlong, jlong, jlong, jboolean, jdouble, jint);

#ifdef __cplusplus
}
#endif
#endif