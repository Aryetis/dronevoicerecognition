#include "matrix.h"

/*******************************************************************************
 *
 * Drone control through voice recognition -- PC to drone communication
 * Team GYTAM, feb. 2016
 *
 * @author Tom Lucas, Grégoire Todeschi
 *
 ******************************************************************************/
 #pragma once

/**

* Dtw function that given two matrix of cep coefficient computes distance

* between those two signals.
*  @param n_ck         Dimension of unknow signal
*  @param n_cunk         Dimension of know signal
*  @param dim_mfcc  Size of nfcc decompostion base
*  @param c_k       Matrix of know signal
*  @param c_unk     Matrix of unknow signal
*  @return Distance between the two signals
* @file
*/
float dtw(int n_ck, int n_cunk, int dim_mfcc, Matrix<float> c_k, Matrix<float> c_unk);
