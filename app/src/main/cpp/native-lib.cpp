#include <jni.h>
#include <string>

extern "C"
JNIEXPORT jstring

JNICALL
Java_openeyes_drawalive_seven_openeyes_MainEyes_stringFromJNI(
        JNIEnv *env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}
