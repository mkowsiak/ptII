# Makefile org sources from outside of Ptolemy
#
# @Version: $Id$
# @Author: Ilge Akkaya
#
# @Copyright (c) 2007-2013 The Regents of the University of California.
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
# 						PT_COPYRIGHT_VERSION_2
# 						COPYRIGHTENDKEY
##########################################################################

# Root of Ptolemy II directory
ROOT = ../../../..

DIRS = demo test

# $(ME) is used in ptdir.mk
ME = org/ptolemy/machineLearning/hmm

CLASSPATH =	$(ROOT)

# True source directory
VPATH =	$(ROOT)/$(ME)

# Get configuration info
CONFIG = $(ROOT)/mk/ptII.mk
include $(CONFIG)

# Used to build jar files
PTPACKAGE = hmm
PTCLASSJAR = $(PTPACKAGE).jar

JSRCS = \
	HMMExponentialClassifier.java \
	HMMExponentialEstimator.java \
	HMMGaussianClassifier.java \
	HMMGaussianEstimator.java \
	HMMMultinomialClassifier.java \
	HMMMultinomialEstimator.java \
	ObservationClassifier.java \
	ParameterEstimator.java

OTHER_FILES_TO_BE_JARED = \
	HMMExponentialClassifierIcon.xml \
	HMMExponentialEstimatorIcon.xml \
	HMMGaussianClassifierIcon.xml \
	HMMGaussianEstimatorIcon.xml \
	HMMMultinomialClassifierIcon.xml \
	HMMMultinomialEstimatorIcon.xml \
	hmm.xml

EXTRA_SRCS = $(JSRCS) $(OTHER_FILES_TO_BE_JARED)

# Sources that may or may not be present, but if they are present, we don't
# want make checkjunk to barf on them.
# Don't include demo or DIRS here, or else 'make sources' will run 'make demo'
MISC_FILES = test

# make checkjunk will not report OPTIONAL_FILES as trash
# make distclean removes OPTIONAL_FILES
OPTIONAL_FILES = demo test\

JCLASS = $(JSRCS:%.java=%.class)

all: jclass
install: jclass jars

# Get the rest of the rules
include $(ROOT)/mk/ptcommon.mk
