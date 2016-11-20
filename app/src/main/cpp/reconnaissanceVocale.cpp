#include <stdio.h>
#include <cstdlib>
#include <cstring>
#include <string>
#include <iostream>
#include <cstdint>
#include <vector>
#include <map>
#include <cfloat>

#include "WavToMfcc.h"
#include "reconnaissanceVocale.h"
#include "dtw.h"

Matrix<float> parametrisation(std::string word) {

    struct wavfile header;
    FILE *file = NULL;

    // lecture du header
    wavRead(&file, word.c_str(), &header);

    // création du tableau de donnée à la bonne taille
    //int16_t *data = (int16_t*) malloc(sizeof(int8_t) * header.bytes_in_data);
    int16_t data[header.bytes_in_data / 2];
    std::cout << "data size " << word << " : " << header.bytes_in_data / 2 << std::endl;

    // positionne à l'octet 44 dans le fichier et lectures des données
    fseek(file, 44, SEEK_SET);
    fread(data, sizeof(int16_t), header.bytes_in_data, file);

    // maintenant que toutes les données on était luent
    // il faut enlever le silence
    int16_t *dataFiltered;
    int dataFilteredLength;

    removeSilence(data, header.bytes_in_data / 2, &dataFiltered, &dataFilteredLength, 0.1);

    // il faut appeler la fonction computeMFCC
    float *mfccResult;
    int mfccLength;
    /**
    * frame_length = 25 ms
    * frame_step = 10 ms
    * dim_mfcc = 13
    * num_filter = 20
    */
    computeMFCC(&mfccResult, &mfccLength, dataFiltered, dataFilteredLength,
        header.frequency, 25, 10, 12, 20);

    // convertion en une matrice
    uint8_t nbLine = 10;
    uint16_t nbCol = mfccLength / 10;
    Matrix<float> output(nbLine, nbCol);

    for (uint16_t c = 0; c < nbCol; ++c)
        for (uint8_t l = 0; l < nbLine; ++l)
            output.set(l, c, mfccResult[l * nbLine + c]);

    return output;
}


std::string buildPath(std::string sdcard, std::string corpus, std::string word) {
    return sdcard + "/Corpus/" + corpus + "_" + word + ".wav";
}