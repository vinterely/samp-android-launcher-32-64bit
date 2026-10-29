//
// Created by GLORY TEAM.
//

#ifndef GLORY_RQVECTOR_H
#define GLORY_RQVECTOR_H


class RQVector {

    union {
        struct {
            float x;
            float y;
            float z;
            float w;
        };
        struct {
            float r;
            float g;
            float b;
            float a;
        };
    };
};


#endif //GLORY_RQVECTOR_H
