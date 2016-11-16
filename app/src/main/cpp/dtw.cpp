/*******************************************************************************
 *
 * Drone control through voice recognition -- PC to drone communication
 * Team GYTAM, feb. 2016
 *
 *
 ******************************************************************************/

#include <iostream>
#include <stdio.h>
#include <stdlib.h>
#include <string.h> // for memcmp
#include <stdint.h> // for int16_t and int32_t
#include <math.h>
#include <iostream>
#include <climits>
#include "dtw.h"
#include "matrix.h"



float minimum(float a, float b) {
    if (a < b) return a;
    return b;
}

float distance(Matrix<float> c_k, Matrix<float> c_unk, int i, int j) {
    // récupération de la colonne i cd c_k and la colone j cd c_unk
    Matrix<float> c_ki = c_k.getColumn(i);
    Matrix<float> c_unkj = c_unk.getColumn(j);

    float diff;          // variables intermédiaires pour le calcul de la différence
    float diffSquare;    // ...
    float result = 0;

    /*
    std::cout << " ================== c_ki column" << std::endl;
    c_ki.draw();
    std::cout << " ================= c_unkj column" << std::endl;
    c_unkj.draw();
    */
    for (int i = 0; i < c_ki.sizeLine(); ++i) {
        diff = c_ki.get(i, 0) - c_unkj.get(i, 0);
        diffSquare = diff * diff;
        result += diffSquare;
    }

    return result;
}


/**
* Dtw function that given two matrix of cep coefficient computes distance
* between those two signals.
*  @param n_ck      Dimension of know signal
*  @param n_cunk    Dimension of unknow signal
*  @param dim_mfcc  Size of nfcc decompostion base
*  @param c_k       Matrix of know signal
*  @param c_unk     Matrix of unknow signal
*  @return Distance between the two signals
*/

float dtw(int n_ck, int n_cunk, int dim_mfcc, Matrix<float> c_k, Matrix<float> c_unk) {
    int I = n_ck;
    int J = n_cunk;
    Matrix<float> g(I, J);

    float W0 = 1;
    float W1 = 1;
    float W2 = 2;
    g.set(0, 0, 0.0);

    for (int j = 1; j < J; ++j)
        g.set(0, j, INT_MAX);

    for (int i = 1; i < I; ++i) {
        g.set(i, 0, INT_MAX);

        for (int j = 1; j < J; ++j) {
            float dist = distance(c_k, c_unk, i-1, j-1);
            float mini = minimum(g.get(i - 1, j) + W0 * dist, g.get(i-1, j-1) + W1 * dist);
            g.set(i, j, minimum(mini, g.get(i, j-1) + W2 * dist));
        }
    }

    return g.get(I-1, J-1) / (I + J)-1;
}
