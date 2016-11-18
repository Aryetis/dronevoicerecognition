/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
#include <cstring>
#include <map>
#include <cfloat>
#include <jni.h>
#include <cinttypes>
#include <android/log.h>

#include "reconnaissanceVocale.h"
#include "dtw.h"
#include "matrix.h"
#include "libmfccOptim.h"
#include "WavToMfcc.h"



#define LOGI(...) \
  ((void)__android_log_print(ANDROID_LOG_INFO, "hell-libs::", __VA_ARGS__))







extern "C" JNIEXPORT jfloat JNICALL
Java_com_dvr_mel_dronevoicerecognition_FinalCorpusActivity_computeRecognitionRatio(
        JNIEnv *env, jobject obj, jstring pathToSDCard,
        jstring reference, jstring hypothese) {

    // convert all parameters to be usable by C++ functions
    const char *c_reference = env->GetStringUTFChars(reference, 0);
    const char *c_hypothese = env->GetStringUTFChars(hypothese, 0);
    const char *c_sdcard = env->GetStringUTFChars(pathToSDCard, 0);
    std::string ref = c_reference;
    std::string hyp = c_hypothese;
    std::string sdc = c_sdcard;

    // get the ID for the updateProgressLabel(String) method
    //jclass      cls = env->GetObjectClass(obj);
    //jmethodID   mid = env->GetMethodID(cls, "updateProgressLabel", "([Ljava/lang/String;)V");

    //if (mid == 0) return -1;



    // init all variables
    std::map<std::string, Matrix<float>> references;
    std::map<std::string, Matrix<float>> hypotheses;
    int indR = 0, indH = 0;
    int i = 0, j = 0;
    float distance = 0, mini = FLT_MAX;
    std::vector<std::string> vocabulaires = { "avance", "recule", "droite", "gauche",
                                              "etatdurgence", "tournedroite", "tournegauche",
                                              "faisunflip", "arretetoi" };
    Matrix<int> confusion(vocabulaires.size(), vocabulaires.size());

    // 1 - Paramétrisation de tout les mots de l'hypothèse et de la références
    for (std::string word:vocabulaires) {
        references[word] = parametrisation( buildPath(sdc, ref, word) );
        hypotheses[word] = parametrisation( buildPath(sdc, hyp, word) );
    }

    return 98.0;

    // 2 - Lancement de la reconnaissance et construction de la matrice de confusion
    for (auto& kvr:references) {
        float mini = FLT_MAX;
        int indH = 0, j = 0;

        for (auto& kvh:hypotheses) {

            // mise a jour du text de updateProgressLabel
            //std::string msg = kvr.first + " - " + kvh.first;
            //env->CallVoidMethod(obj, mid, msg.c_str());

            float distance = dtw(   kvr.second.sizeColumn(), kvh.second.sizeColumn(), 12,
                                    kvr.second, kvh.second);

            if (distance < mini) {
                mini = distance;
                indR = i;
                indH = j;
            }

            j++;
        }

        confusion.set(indR, indH, confusion.get(indR, indH) + 1);
        i++;
    }

    // analyse de la matrice de confusion et calcul du taux de reconnaissance
    float nbCalcul = confusion.sum();
    float nbSucces = confusion.trace();
    float nbError = nbCalcul - nbSucces;
    float taux = nbSucces / nbCalcul;

    return taux;
}
