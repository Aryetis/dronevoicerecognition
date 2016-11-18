#ifndef __RECONNAISSANCE_VOCALE_H__
#define __RECONNAISSANCE_VOCALE_H__

#include <string>

#include "matrix.h"

#define CORPUS_DIRECTORY    "./corpus/dronevolant_bruite"

Matrix<float> parametrisation(std::string word);
std::string buildPath(std::string init, std::string locuteur, std::string word);



#endif
