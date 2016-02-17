/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class jdd_JDD */

#ifndef _Included_jdd_JDD
#define _Included_jdd_JDD
#ifdef __cplusplus
extern "C" {
#endif
#undef jdd_JDD_PLUS
#define jdd_JDD_PLUS 1L
#undef jdd_JDD_MINUS
#define jdd_JDD_MINUS 2L
#undef jdd_JDD_TIMES
#define jdd_JDD_TIMES 3L
#undef jdd_JDD_DIVIDE
#define jdd_JDD_DIVIDE 4L
#undef jdd_JDD_MIN
#define jdd_JDD_MIN 5L
#undef jdd_JDD_MAX
#define jdd_JDD_MAX 6L
#undef jdd_JDD_EQUALS
#define jdd_JDD_EQUALS 7L
#undef jdd_JDD_NOTEQUALS
#define jdd_JDD_NOTEQUALS 8L
#undef jdd_JDD_GREATERTHAN
#define jdd_JDD_GREATERTHAN 9L
#undef jdd_JDD_GREATERTHANEQUALS
#define jdd_JDD_GREATERTHANEQUALS 10L
#undef jdd_JDD_LESSTHAN
#define jdd_JDD_LESSTHAN 11L
#undef jdd_JDD_LESSTHANEQUALS
#define jdd_JDD_LESSTHANEQUALS 12L
#undef jdd_JDD_FLOOR
#define jdd_JDD_FLOOR 13L
#undef jdd_JDD_CEIL
#define jdd_JDD_CEIL 14L
#undef jdd_JDD_POW
#define jdd_JDD_POW 15L
#undef jdd_JDD_MOD
#define jdd_JDD_MOD 16L
#undef jdd_JDD_LOGXY
#define jdd_JDD_LOGXY 17L
#undef jdd_JDD_ZERO_ONE
#define jdd_JDD_ZERO_ONE 1L
#undef jdd_JDD_LOW
#define jdd_JDD_LOW 2L
#undef jdd_JDD_NORMAL
#define jdd_JDD_NORMAL 3L
#undef jdd_JDD_HIGH
#define jdd_JDD_HIGH 4L
#undef jdd_JDD_LIST
#define jdd_JDD_LIST 5L
#undef jdd_JDD_CMU
#define jdd_JDD_CMU 1L
#undef jdd_JDD_BOULDER
#define jdd_JDD_BOULDER 2L
/*
 * Class:     jdd_JDD
 * Method:    GetCUDDManager
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL Java_jdd_JDD_GetCUDDManager
  (JNIEnv *, jclass);

/*
 * Class:     jdd_JDD
 * Method:    DD_SetOutputStream
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_jdd_JDD_DD_1SetOutputStream
  (JNIEnv *, jclass, jlong);

/*
 * Class:     jdd_JDD
 * Method:    DD_GetOutputStream
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL Java_jdd_JDD_DD_1GetOutputStream
  (JNIEnv *, jclass);

/*
 * Class:     jdd_JDD
 * Method:    DD_InitialiseCUDD
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_jdd_JDD_DD_1InitialiseCUDD__
  (JNIEnv *, jclass);

/*
 * Class:     jdd_JDD
 * Method:    DD_InitialiseCUDD
 * Signature: (JD)V
 */
JNIEXPORT void JNICALL Java_jdd_JDD_DD_1InitialiseCUDD__JD
  (JNIEnv *, jclass, jlong, jdouble);

/*
 * Class:     jdd_JDD
 * Method:    DD_SetCUDDMaxMem
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_jdd_JDD_DD_1SetCUDDMaxMem
  (JNIEnv *, jclass, jlong);

/*
 * Class:     jdd_JDD
 * Method:    DD_SetCUDDEpsilon
 * Signature: (D)V
 */
JNIEXPORT void JNICALL Java_jdd_JDD_DD_1SetCUDDEpsilon
  (JNIEnv *, jclass, jdouble);

/*
 * Class:     jdd_JDD
 * Method:    DD_CloseDownCUDD
 * Signature: (Z)V
 */
JNIEXPORT void JNICALL Java_jdd_JDD_DD_1CloseDownCUDD
  (JNIEnv *, jclass, jboolean);

/*
 * Class:     jdd_JDD
 * Method:    DD_Ref
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_jdd_JDD_DD_1Ref
  (JNIEnv *, jclass, jlong);

/*
 * Class:     jdd_JDD
 * Method:    DD_Deref
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_jdd_JDD_DD_1Deref
  (JNIEnv *, jclass, jlong);

/*
 * Class:     jdd_JDD
 * Method:    DD_PrintCacheInfo
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_jdd_JDD_DD_1PrintCacheInfo
  (JNIEnv *, jclass);

/*
 * Class:     jdd_JDD
 * Method:    DD_GetErrorFlag
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_jdd_JDD_DD_1GetErrorFlag
  (JNIEnv *, jclass);

/*
 * Class:     jdd_JDD
 * Method:    DD_Reorder
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_jdd_JDD_DD_1Reorder
  (JNIEnv *, jclass);

/*
 * Class:     jdd_JDD
 * Method:    DD_ResetVarOrder
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_jdd_JDD_DD_1ResetVarOrder
  (JNIEnv *, jclass);

/*
 * Class:     jdd_JDD
 * Method:    DD_Create
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL Java_jdd_JDD_DD_1Create
  (JNIEnv *, jclass);

/*
 * Class:     jdd_JDD
 * Method:    DD_Constant
 * Signature: (D)J
 */
JNIEXPORT jlong JNICALL Java_jdd_JDD_DD_1Constant
  (JNIEnv *, jclass, jdouble);

/*
 * Class:     jdd_JDD
 * Method:    DD_PlusInfinity
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL Java_jdd_JDD_DD_1PlusInfinity
  (JNIEnv *, jclass);

/*
 * Class:     jdd_JDD
 * Method:    DD_MinusInfinity
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL Java_jdd_JDD_DD_1MinusInfinity
  (JNIEnv *, jclass);

/*
 * Class:     jdd_JDD
 * Method:    DD_Var
 * Signature: (I)J
 */
JNIEXPORT jlong JNICALL Java_jdd_JDD_DD_1Var
  (JNIEnv *, jclass, jint);

/*
 * Class:     jdd_JDD
 * Method:    DD_Not
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_jdd_JDD_DD_1Not
  (JNIEnv *, jclass, jlong);

/*
 * Class:     jdd_JDD
 * Method:    DD_Or
 * Signature: (JJ)J
 */
JNIEXPORT jlong JNICALL Java_jdd_JDD_DD_1Or
  (JNIEnv *, jclass, jlong, jlong);

/*
 * Class:     jdd_JDD
 * Method:    DD_And
 * Signature: (JJ)J
 */
JNIEXPORT jlong JNICALL Java_jdd_JDD_DD_1And
  (JNIEnv *, jclass, jlong, jlong);

/*
 * Class:     jdd_JDD
 * Method:    DD_Xor
 * Signature: (JJ)J
 */
JNIEXPORT jlong JNICALL Java_jdd_JDD_DD_1Xor
  (JNIEnv *, jclass, jlong, jlong);

/*
 * Class:     jdd_JDD
 * Method:    DD_Implies
 * Signature: (JJ)J
 */
JNIEXPORT jlong JNICALL Java_jdd_JDD_DD_1Implies
  (JNIEnv *, jclass, jlong, jlong);

/*
 * Class:     jdd_JDD
 * Method:    DD_Apply
 * Signature: (IJJ)J
 */
JNIEXPORT jlong JNICALL Java_jdd_JDD_DD_1Apply
  (JNIEnv *, jclass, jint, jlong, jlong);

/*
 * Class:     jdd_JDD
 * Method:    DD_MonadicApply
 * Signature: (IJ)J
 */
JNIEXPORT jlong JNICALL Java_jdd_JDD_DD_1MonadicApply
  (JNIEnv *, jclass, jint, jlong);

/*
 * Class:     jdd_JDD
 * Method:    DD_Restrict
 * Signature: (JJ)J
 */
JNIEXPORT jlong JNICALL Java_jdd_JDD_DD_1Restrict
  (JNIEnv *, jclass, jlong, jlong);

/*
 * Class:     jdd_JDD
 * Method:    DD_ITE
 * Signature: (JJJ)J
 */
JNIEXPORT jlong JNICALL Java_jdd_JDD_DD_1ITE
  (JNIEnv *, jclass, jlong, jlong, jlong);

/*
 * Class:     jdd_JDD
 * Method:    DD_PermuteVariables
 * Signature: (JJJI)J
 */
JNIEXPORT jlong JNICALL Java_jdd_JDD_DD_1PermuteVariables
  (JNIEnv *, jclass, jlong, jlong, jlong, jint);

/*
 * Class:     jdd_JDD
 * Method:    DD_SwapVariables
 * Signature: (JJJI)J
 */
JNIEXPORT jlong JNICALL Java_jdd_JDD_DD_1SwapVariables
  (JNIEnv *, jclass, jlong, jlong, jlong, jint);

/*
 * Class:     jdd_JDD
 * Method:    DD_VariablesGreaterThan
 * Signature: (JJI)J
 */
JNIEXPORT jlong JNICALL Java_jdd_JDD_DD_1VariablesGreaterThan
  (JNIEnv *, jclass, jlong, jlong, jint);

/*
 * Class:     jdd_JDD
 * Method:    DD_VariablesGreaterThanEquals
 * Signature: (JJI)J
 */
JNIEXPORT jlong JNICALL Java_jdd_JDD_DD_1VariablesGreaterThanEquals
  (JNIEnv *, jclass, jlong, jlong, jint);

/*
 * Class:     jdd_JDD
 * Method:    DD_VariablesLessThan
 * Signature: (JJI)J
 */
JNIEXPORT jlong JNICALL Java_jdd_JDD_DD_1VariablesLessThan
  (JNIEnv *, jclass, jlong, jlong, jint);

/*
 * Class:     jdd_JDD
 * Method:    DD_VariablesLessThanEquals
 * Signature: (JJI)J
 */
JNIEXPORT jlong JNICALL Java_jdd_JDD_DD_1VariablesLessThanEquals
  (JNIEnv *, jclass, jlong, jlong, jint);

/*
 * Class:     jdd_JDD
 * Method:    DD_VariablesEquals
 * Signature: (JJI)J
 */
JNIEXPORT jlong JNICALL Java_jdd_JDD_DD_1VariablesEquals
  (JNIEnv *, jclass, jlong, jlong, jint);

/*
 * Class:     jdd_JDD
 * Method:    DD_ThereExists
 * Signature: (JJI)J
 */
JNIEXPORT jlong JNICALL Java_jdd_JDD_DD_1ThereExists
  (JNIEnv *, jclass, jlong, jlong, jint);

/*
 * Class:     jdd_JDD
 * Method:    DD_ForAll
 * Signature: (JJI)J
 */
JNIEXPORT jlong JNICALL Java_jdd_JDD_DD_1ForAll
  (JNIEnv *, jclass, jlong, jlong, jint);

/*
 * Class:     jdd_JDD
 * Method:    DD_SumAbstract
 * Signature: (JJI)J
 */
JNIEXPORT jlong JNICALL Java_jdd_JDD_DD_1SumAbstract
  (JNIEnv *, jclass, jlong, jlong, jint);

/*
 * Class:     jdd_JDD
 * Method:    DD_ProductAbstract
 * Signature: (JJI)J
 */
JNIEXPORT jlong JNICALL Java_jdd_JDD_DD_1ProductAbstract
  (JNIEnv *, jclass, jlong, jlong, jint);

/*
 * Class:     jdd_JDD
 * Method:    DD_MinAbstract
 * Signature: (JJI)J
 */
JNIEXPORT jlong JNICALL Java_jdd_JDD_DD_1MinAbstract
  (JNIEnv *, jclass, jlong, jlong, jint);

/*
 * Class:     jdd_JDD
 * Method:    DD_MaxAbstract
 * Signature: (JJI)J
 */
JNIEXPORT jlong JNICALL Java_jdd_JDD_DD_1MaxAbstract
  (JNIEnv *, jclass, jlong, jlong, jint);

/*
 * Class:     jdd_JDD
 * Method:    DD_GreaterThan
 * Signature: (JD)J
 */
JNIEXPORT jlong JNICALL Java_jdd_JDD_DD_1GreaterThan
  (JNIEnv *, jclass, jlong, jdouble);

/*
 * Class:     jdd_JDD
 * Method:    DD_GreaterThanEquals
 * Signature: (JD)J
 */
JNIEXPORT jlong JNICALL Java_jdd_JDD_DD_1GreaterThanEquals
  (JNIEnv *, jclass, jlong, jdouble);

/*
 * Class:     jdd_JDD
 * Method:    DD_LessThan
 * Signature: (JD)J
 */
JNIEXPORT jlong JNICALL Java_jdd_JDD_DD_1LessThan
  (JNIEnv *, jclass, jlong, jdouble);

/*
 * Class:     jdd_JDD
 * Method:    DD_LessThanEquals
 * Signature: (JD)J
 */
JNIEXPORT jlong JNICALL Java_jdd_JDD_DD_1LessThanEquals
  (JNIEnv *, jclass, jlong, jdouble);

/*
 * Class:     jdd_JDD
 * Method:    DD_Equals
 * Signature: (JD)J
 */
JNIEXPORT jlong JNICALL Java_jdd_JDD_DD_1Equals
  (JNIEnv *, jclass, jlong, jdouble);

/*
 * Class:     jdd_JDD
 * Method:    DD_Interval
 * Signature: (JDD)J
 */
JNIEXPORT jlong JNICALL Java_jdd_JDD_DD_1Interval
  (JNIEnv *, jclass, jlong, jdouble, jdouble);

/*
 * Class:     jdd_JDD
 * Method:    DD_RoundOff
 * Signature: (JI)J
 */
JNIEXPORT jlong JNICALL Java_jdd_JDD_DD_1RoundOff
  (JNIEnv *, jclass, jlong, jint);

/*
 * Class:     jdd_JDD
 * Method:    DD_EqualSupNorm
 * Signature: (JJD)Z
 */
JNIEXPORT jboolean JNICALL Java_jdd_JDD_DD_1EqualSupNorm
  (JNIEnv *, jclass, jlong, jlong, jdouble);

/*
 * Class:     jdd_JDD
 * Method:    DD_FindMin
 * Signature: (J)D
 */
JNIEXPORT jdouble JNICALL Java_jdd_JDD_DD_1FindMin
  (JNIEnv *, jclass, jlong);

/*
 * Class:     jdd_JDD
 * Method:    DD_FindMax
 * Signature: (J)D
 */
JNIEXPORT jdouble JNICALL Java_jdd_JDD_DD_1FindMax
  (JNIEnv *, jclass, jlong);

/*
 * Class:     jdd_JDD
 * Method:    DD_RestrictToFirst
 * Signature: (JJI)J
 */
JNIEXPORT jlong JNICALL Java_jdd_JDD_DD_1RestrictToFirst
  (JNIEnv *, jclass, jlong, jlong, jint);

/*
 * Class:     jdd_JDD
 * Method:    DD_GetNumNodes
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_jdd_JDD_DD_1GetNumNodes
  (JNIEnv *, jclass, jlong);

/*
 * Class:     jdd_JDD
 * Method:    DD_GetNumTerminals
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_jdd_JDD_DD_1GetNumTerminals
  (JNIEnv *, jclass, jlong);

/*
 * Class:     jdd_JDD
 * Method:    DD_GetNumMinterms
 * Signature: (JI)D
 */
JNIEXPORT jdouble JNICALL Java_jdd_JDD_DD_1GetNumMinterms
  (JNIEnv *, jclass, jlong, jint);

/*
 * Class:     jdd_JDD
 * Method:    DD_GetNumPaths
 * Signature: (J)D
 */
JNIEXPORT jdouble JNICALL Java_jdd_JDD_DD_1GetNumPaths
  (JNIEnv *, jclass, jlong);

/*
 * Class:     jdd_JDD
 * Method:    DD_PrintInfo
 * Signature: (JI)V
 */
JNIEXPORT void JNICALL Java_jdd_JDD_DD_1PrintInfo
  (JNIEnv *, jclass, jlong, jint);

/*
 * Class:     jdd_JDD
 * Method:    DD_PrintInfoBrief
 * Signature: (JI)V
 */
JNIEXPORT void JNICALL Java_jdd_JDD_DD_1PrintInfoBrief
  (JNIEnv *, jclass, jlong, jint);

/*
 * Class:     jdd_JDD
 * Method:    DD_PrintSupport
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_jdd_JDD_DD_1PrintSupport
  (JNIEnv *, jclass, jlong);

/*
 * Class:     jdd_JDD
 * Method:    DD_PrintSupportNames
 * Signature: (JLjava/util/List;)V
 */
JNIEXPORT void JNICALL Java_jdd_JDD_DD_1PrintSupportNames
  (JNIEnv *, jclass, jlong, jobject);

/*
 * Class:     jdd_JDD
 * Method:    DD_GetSupport
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_jdd_JDD_DD_1GetSupport
  (JNIEnv *, jclass, jlong);

/*
 * Class:     jdd_JDD
 * Method:    DD_PrintTerminals
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_jdd_JDD_DD_1PrintTerminals
  (JNIEnv *, jclass, jlong);

/*
 * Class:     jdd_JDD
 * Method:    DD_PrintTerminalsAndNumbers
 * Signature: (JI)V
 */
JNIEXPORT void JNICALL Java_jdd_JDD_DD_1PrintTerminalsAndNumbers
  (JNIEnv *, jclass, jlong, jint);

/*
 * Class:     jdd_JDD
 * Method:    DD_SetVectorElement
 * Signature: (JJIJD)J
 */
JNIEXPORT jlong JNICALL Java_jdd_JDD_DD_1SetVectorElement
  (JNIEnv *, jclass, jlong, jlong, jint, jlong, jdouble);

/*
 * Class:     jdd_JDD
 * Method:    DD_SetMatrixElement
 * Signature: (JJIJIJJD)J
 */
JNIEXPORT jlong JNICALL Java_jdd_JDD_DD_1SetMatrixElement
  (JNIEnv *, jclass, jlong, jlong, jint, jlong, jint, jlong, jlong, jdouble);

/*
 * Class:     jdd_JDD
 * Method:    DD_Set3DMatrixElement
 * Signature: (JJIJIJIJJJD)J
 */
JNIEXPORT jlong JNICALL Java_jdd_JDD_DD_1Set3DMatrixElement
  (JNIEnv *, jclass, jlong, jlong, jint, jlong, jint, jlong, jint, jlong, jlong, jlong, jdouble);

/*
 * Class:     jdd_JDD
 * Method:    DD_GetVectorElement
 * Signature: (JJIJ)D
 */
JNIEXPORT jdouble JNICALL Java_jdd_JDD_DD_1GetVectorElement
  (JNIEnv *, jclass, jlong, jlong, jint, jlong);

/*
 * Class:     jdd_JDD
 * Method:    DD_Identity
 * Signature: (JJI)J
 */
JNIEXPORT jlong JNICALL Java_jdd_JDD_DD_1Identity
  (JNIEnv *, jclass, jlong, jlong, jint);

/*
 * Class:     jdd_JDD
 * Method:    DD_Transpose
 * Signature: (JJJI)J
 */
JNIEXPORT jlong JNICALL Java_jdd_JDD_DD_1Transpose
  (JNIEnv *, jclass, jlong, jlong, jlong, jint);

/*
 * Class:     jdd_JDD
 * Method:    DD_MatrixMultiply
 * Signature: (JJJII)J
 */
JNIEXPORT jlong JNICALL Java_jdd_JDD_DD_1MatrixMultiply
  (JNIEnv *, jclass, jlong, jlong, jlong, jint, jint);

/*
 * Class:     jdd_JDD
 * Method:    DD_PrintVector
 * Signature: (JJII)V
 */
JNIEXPORT void JNICALL Java_jdd_JDD_DD_1PrintVector
  (JNIEnv *, jclass, jlong, jlong, jint, jint);

/*
 * Class:     jdd_JDD
 * Method:    DD_PrintMatrix
 * Signature: (JJIJII)V
 */
JNIEXPORT void JNICALL Java_jdd_JDD_DD_1PrintMatrix
  (JNIEnv *, jclass, jlong, jlong, jint, jlong, jint, jint);

/*
 * Class:     jdd_JDD
 * Method:    DD_PrintVectorFiltered
 * Signature: (JJJII)V
 */
JNIEXPORT void JNICALL Java_jdd_JDD_DD_1PrintVectorFiltered
  (JNIEnv *, jclass, jlong, jlong, jlong, jint, jint);

/*
 * Class:     jdd_JDD
 * Method:    DD_ExportDDToDotFile
 * Signature: (JLjava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_jdd_JDD_DD_1ExportDDToDotFile
  (JNIEnv *, jclass, jlong, jstring);

/*
 * Class:     jdd_JDD
 * Method:    DD_ExportDDToDotFileLabelled
 * Signature: (JLjava/lang/String;Ljava/util/List;)V
 */
JNIEXPORT void JNICALL Java_jdd_JDD_DD_1ExportDDToDotFileLabelled
  (JNIEnv *, jclass, jlong, jstring, jobject);

/*
 * Class:     jdd_JDD
 * Method:    DD_ExportMatrixToPPFile
 * Signature: (JJIJILjava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_jdd_JDD_DD_1ExportMatrixToPPFile
  (JNIEnv *, jclass, jlong, jlong, jint, jlong, jint, jstring);

/*
 * Class:     jdd_JDD
 * Method:    DD_Export3dMatrixToPPFile
 * Signature: (JJIJIJILjava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_jdd_JDD_DD_1Export3dMatrixToPPFile
  (JNIEnv *, jclass, jlong, jlong, jint, jlong, jint, jlong, jint, jstring);

/*
 * Class:     jdd_JDD
 * Method:    DD_ExportMatrixToMatlabFile
 * Signature: (JJIJILjava/lang/String;Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_jdd_JDD_DD_1ExportMatrixToMatlabFile
  (JNIEnv *, jclass, jlong, jlong, jint, jlong, jint, jstring, jstring);

/*
 * Class:     jdd_JDD
 * Method:    DD_ExportMatrixToSpyFile
 * Signature: (JJIJIILjava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_jdd_JDD_DD_1ExportMatrixToSpyFile
  (JNIEnv *, jclass, jlong, jlong, jint, jlong, jint, jint, jstring);

#ifdef __cplusplus
}
#endif
#endif
/* Header for class jdd_JDD_CuddOutOfMemoryException */

#ifndef _Included_jdd_JDD_CuddOutOfMemoryException
#define _Included_jdd_JDD_CuddOutOfMemoryException
#ifdef __cplusplus
extern "C" {
#endif
#undef jdd_JDD_CuddOutOfMemoryException_serialVersionUID
#define jdd_JDD_CuddOutOfMemoryException_serialVersionUID -3042686055658047285LL
#undef jdd_JDD_CuddOutOfMemoryException_serialVersionUID
#define jdd_JDD_CuddOutOfMemoryException_serialVersionUID -3387516993124229948LL
#undef jdd_JDD_CuddOutOfMemoryException_serialVersionUID
#define jdd_JDD_CuddOutOfMemoryException_serialVersionUID -7034897190745766939LL
#undef jdd_JDD_CuddOutOfMemoryException_serialVersionUID
#define jdd_JDD_CuddOutOfMemoryException_serialVersionUID -3094099053041270477LL
#ifdef __cplusplus
}
#endif
#endif
