# Makefile for Ptolemy II - HLA/CERTI library
#
# @Authors: Gilles Lasnier, based on a file by Christopher H. Brooks
#
# @Version: $Id$
#
# @Copyright (c) 2013-2018 The Regents of the University of California.
# All rights reserved.
#
# Permission is hereby granted, without written agreement and without
# license or royalty fees, to use, copy, modify, and distribute this
# software and its documentation for any purpose, provided that the
# above copyright notice and the following two paragraphs appear in all
# copies of this software.
#
# IN NO EVENT SHALL THE UNIVERSITY OF CALIFORNIA BE LIABLE TO ANY PARTY
# FOR DIRECT, INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES
# ARISING OUT OF THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN IF
# THE UNIVERSITY OF CALIFORNIA HAS BEEN ADVISED OF THE POSSIBILITY OF
# SUCH DAMAGE.
#
# THE UNIVERSITY OF CALIFORNIA SPECIFICALLY DISCLAIMS ANY WARRANTIES,
# INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
# MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. THE SOFTWARE
# PROVIDED HEREUNDER IS ON AN "AS IS" BASIS, AND THE UNIVERSITY OF
# CALIFORNIA HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES,
# ENHANCEMENTS, OR MODIFICATIONS.
#
#                       PT_COPYRIGHT_VERSION_2
#                       COPYRIGHTENDKEY

ME =        org/hlacerti/lib

DIRS =      #test

# Root of the Java directory
ROOT =      ../../..

# PTJCERTI_JAR is set to $PTII/lib/jcerti.jar by configure.
CLASSPATH = $(ROOT)$(CLASSPATHSEPARATOR)$(PTJCERTI_JAR)$(CLASSPATHSEPARATOR)$(PTJSOUP_JAR)

# Get configuration info
CONFIG =    $(ROOT)/mk/ptII.mk
include $(CONFIG)

# Used to build jar files
PTPACKAGE =     lib
PTCLASSJAR =

# Include the .class files from these jars in PTCLASSALLJAR
PTCLASSALLJARS =

PTCLASSALLJAR = $(PTPACKAGE).jar

JSRCS = \
	AutomaticSimulation.java \
	CertiRtig.java \
	HlaAttributeReflector.java \
	HlaAttributeUpdater.java \
	HlaManager.java \
	HlaPublisher.java \
	HlaReflectable.java \
	HlaSubscriber.java \
	HlaUpdatable.java \
    MessageProcessing.java \
	HlaTimedEvent.java  

OTHER_FILES_TO_BE_JARED = \
	hlaentities.xml \
	HlaManagerIcon.xml \
	HlaPublisherIcon.xml \
	HlaSubscriberIcon.xml

EXTRA_SRCS =    $(JSRCS) $(OTHER_FILES_TO_BE_JARED)

# Sources that may or may not be present, but if they are present, we don't
# want make checkjunk to barf on them.
MISC_FILES = #test

# make checkjunk will not report OPTIONAL_FILES as trash
# make distclean removes OPTIONAL_FILES
OPTIONAL_FILES = \
	'AutomaticSimulation$$1.class' \
	'AutomaticSimulation$$2.class' \
	'AutomaticSimulation$$3.class' \
	'CertiRtig$$_StreamReaderThread.class' \
	'HlaManager$$ETimeManagementService.class' \
	'HlaManager$$PtolemyFederateAmbassadorInner.class' \
	'HlaManager$$1.class' \
	'HlaManager$$PtolemyFederateAmbassadorInner$$1.class' \
	'HlaSubscriber$$1.class'
JCLASS = $(JSRCS:%.java=%.class)


all: jclass
install: jclass jars


# Get the rest of the rules
include $(ROOT)/mk/ptcommon.mk
