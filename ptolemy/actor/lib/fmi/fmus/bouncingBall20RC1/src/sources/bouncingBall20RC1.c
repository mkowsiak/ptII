/* ---------------------------------------------------------------------------*
 * Sample implementation of an FMU - a bouncing ball. 
 * This demonstrates the use of state events and reinit of states.
 * Equations:
 *  der(h) = v;
 *  der(v) = -g;
 *  when h<0 then v := -e * v;
 *  where
 *    h      height [m], used as state, start = 1
 *    v      velocity of ball [m/s], used as state
 *    der(h) velocity of ball [m/s]
 *    der(v) acceleration of ball [m/s2]
 *    g      acceleration of gravity [m/s2], a parameter, start = 9.81
 *    e      a dimensionless parameter, start = 0.7
 *
 * Copyright QTronic GmbH. All rights reserved.
 * ---------------------------------------------------------------------------*/

// define class name and unique id
#define MODEL_IDENTIFIER bouncingBall20RC1
#define MODEL_GUID "{8c4e810f-3df3-4a00-8276-176fa3c9f004}"

// define model size
#define NUMBER_OF_REALS 6
#define NUMBER_OF_INTEGERS 0
#define NUMBER_OF_BOOLEANS 0
#define NUMBER_OF_STRINGS 0
#define NUMBER_OF_STATES 2
#define NUMBER_OF_EVENT_INDICATORS 1

// We require that functions have prefixes for Linux.
#if defined _WIN32 || defined __CYGWIN__
/* Note: both gcc & MSVC on Windows support this syntax. */
#define FMI_Export __declspec(dllexport)
#else
  #if __GNUC__ >= 4
#define FMI_Export __attribute__ ((visibility ("default")))
  #else
    #define FMI_Export
  #endif
#endif // _WIN32 || defined __CYGWIN__

// include fmu header files, typedefs and macros
#include "fmuTemplate.h"

// define all model variables and their value references
// conventions used here:
// - if x is a variable, then macro x_ is its variable reference
// - the vr of a variable is its index in array  r, i, b or s
// - if k is the vr of a real state, then k+1 is the vr of its derivative
#define h_      0
#define der_h_  1
#define v_      2
#define der_v_  3
#define g_      4
#define e_      5

// define initial state vector as vector of value references
#define STATES { h_, v_ }

// called by fmiInstantiate
// Set values for all variables that define a start value
// Settings used unless changed by fmiSetX before fmiEnterInitializationMode
void setStartValues(ModelInstance *comp) {
    r(h_)     =  1;
    r(v_)     =  0;
    r(der_v_) = -9.81;
    r(g_)     =  9.81;
    r(e_)     =  0.7;
    pos(0) = r(h_) > 0;
}

// called by fmiGetReal, fmiGetContinuousStates and fmiGetDerivatives
fmiReal getReal(ModelInstance* comp, fmiValueReference vr){
    switch (vr) {
        case h_     : return r(h_);
        case der_h_ : return r(v_);
        case v_     : return r(v_);
        case der_v_ : return r(der_v_);
        case g_     : return r(g_);
        case e_     : return r(e_);
        default: return 0;
    }
}

// called by fmiExitInitializationMode() after setting eventInfo to defaults
// Used to set the first time event, if any.
void initialize(ModelInstance* comp, fmiEventInfo* eventInfo) {
    r(der_v_) = -r(g_);
}

// offset for event indicator, adds hysteresis and prevents z=0 at restart 
#define EPS_INDICATORS 1e-14

fmiReal getEventIndicator(ModelInstance* comp, int z) {
    switch (z) {
        case 0 : return r(h_) + (pos(0) ? EPS_INDICATORS : -EPS_INDICATORS);
        default: return 0;
    }
}

// used to set the next time event, if any.
void eventUpdate(ModelInstance* comp, fmiEventInfo* eventInfo) {
    if (pos(0)) {
        r(v_) = - r(e_) * r(v_);
    }
    pos(0) = r(h_) > 0;
    eventInfo->valuesOfContinuousStatesChanged   = fmiTrue;
    eventInfo->nominalsOfContinuousStatesChanged = fmiFalse;
    eventInfo->terminateSimulation   = fmiFalse;
    eventInfo->nextEventTimeDefined  = fmiFalse;
}

// include code that implements the FMI based on the above definitions
#include "fmuTemplate.c"
