/******************************************************************************/
/** Event definition for PtidyOS

 Copyright (c) 1997-2008 The Regents of the University of California.
 All rights reserved.
 Permission is hereby granted, without written agreement and without
 license or royalty fees, to use, copy, modify, and distribute this
 software and its documentation for any purpose, provided that the above
 copyright notice and the following two paragraphs appear in all copies
 of this software.

 IN NO EVENT SHALL THE UNIVERSITY OF CALIFORNIA BE LIABLE TO ANY PARTY
 FOR DIRECT, INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES
 ARISING OUT OF THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN IF
 THE UNIVERSITY OF CALIFORNIA HAS BEEN ADVISED OF THE POSSIBILITY OF
 SUCH DAMAGE.

 THE UNIVERSITY OF CALIFORNIA SPECIFICALLY DISCLAIMS ANY WARRANTIES,
 INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. THE SOFTWARE
 PROVIDED HEREUNDER IS ON AN "AS IS" BASIS, AND THE UNIVERSITY OF
 CALIFORNIA HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES,
 ENHANCEMENTS, OR MODIFICATIONS.

 PT_COPYRIGHT_VERSION_2
 COPYRIGHTENDKEY

 @author Jia Zou and Shanna-Shaye Forbes
 @Parts of this code that serves to interface the hardware is copied from
 previous written code samples by Slobodan Matic and Agilent

 */
/******************************************************************************/

void addEvent(Event*);
unsigned int alreadyFiring(Actor*);
void currentlyFiring(Actor*);
void die(char *mess);
void disableInterrupts(void);
void enableInterrupts(void);
void fireActor(Event*);
void freeEvent(Event*);
long long getCurrentPhysicalTime(void);
unsigned int higherPriority(Actor*);
void IEEE1588Init(void);
void initializeMemory(void);
void interruptInit(void);
Event* newEvent(void);
void processEvents(void);
void processAvailableEvents(void);
void ptpd_systick_init(void); 
void ptpd_init(void);
void removeEvent(void);
long long safeToProcess(Event*);
void setActuationInterrupt(long long);
void setFiringActor(Actor*);
void setTimedInterrupt(long long);
void timerInit(void);
long deadline(Actor *this_actor);
long min(long val1, long val2);
