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



Matrix<int> computeRecognitionOne(  std::string sdc, std::string ref,
                                    std::string hyp);





extern "C" JNIEXPORT jfloat JNICALL
Java_com_dvr_mel_dronevoicerecognition_FinalCorpusActivity_computeRecognitionRatio(
        JNIEnv *env, jobject obj,
        jstring pathToSDCard, jobjectArray references, jstring hypothese) {

    // convertion des paramètres -------------------------------------------------------------------
    std::vector<std::string> v_references;
    std::string s_pathToSDCard, s_hypothese;

    s_pathToSDCard  = env->GetStringUTFChars(pathToSDCard, 0);
    s_hypothese     = env->GetStringUTFChars(hypothese, 0);
    env->ReleaseStringUTFChars(pathToSDCard, s_pathToSDCard.c_str());
    env->ReleaseStringUTFChars(hypothese, s_hypothese.c_str());

    jsize nbReference = env->GetArrayLength(references);
    for (int i = 0; i < nbReference; ++i) {
        jstring         j_tmp = (jstring) env->GetObjectArrayElement(references, i);
        std::string     s_tmp = env->GetStringUTFChars(j_tmp, 0);

        v_references.push_back(s_tmp);

        env->ReleaseStringUTFChars(j_tmp, s_tmp.c_str());
    }

    // lancement de l'algorithme sur toutes les references -----------------------------------------
    Matrix<int> confusion(vocabulaires.size(), vocabulaires.size());

    for (int i = 0; i < v_references.size(); ++i) {
        Matrix<int> tmp = computeRecognitionOne(s_pathToSDCard, v_references[i], s_hypothese);
        confusion = confusion + tmp;
    }

    // Calcul du taux de reconnaissance ------------------------------------------------------------

    float nbCalcul = confusion.sum();
    float nbSuccess = confusion.trace();
    float ratio = nbSuccess / nbCalcul;

    return ratio;
}


/**
 * Permet de calculer la matrice de confusion calculer à partir d'un corpus hypothese et d'une
 * seule référence. Cette matrice de confusion sera utilise plus tard pour calculer la taux de
 * reconnaissance dans un systeme multilocuteur
 */
Matrix<int> computeRecognitionOne(  std::string sdc, std::string ref,
                                    std::string hyp) {

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

    Matrix<int> confusion(vocabulaires.size(), vocabulaires.size());

    // 1 - Paramétrisation de tout les mots de l'hypothèse et de la références
    for (std::string word:vocabulaires) {
        references[word] = parametrisation( buildPath(sdc, ref, word) );
        hypotheses[word] = parametrisation( buildPath(sdc, hyp, word) );
    }

    // 2 - Lancement de la reconnaissance et construction de la matrice de confusion
    for (auto& kvr:references) {
        float mini = FLT_MAX;
        int indH = 0, j = 0;

        for (auto& kvh:hypotheses) {

            // mise a jour du text de updateProgressLabel
            //std::string msg = kvr.first + " - " + kvh.first;
            //env->CallVoidMethod(obj, mid, msg.c_str());

            /* Since the input are linked, staticaly i cannot used directely each percent to calculate
             * the final value. I need to get all the confusion matrix and add them together to
             * be able to calculate properly the succes perent
             */
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

    return confusion;
}
