/* Implement the QSS3 method for solving ordinary differential equations.

Copyright (c) 2014 The Regents of the University of California.
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
*/


package org.ptolemy.qss.solver;


import org.ptolemy.qss.util.ModelPolynomial;

import ptolemy.actor.util.Time;


//////////////////////////////////////////////////////////////////////////
//// QSS3Fd


/** Implement the QSS3 method for solving ordinary differential equations.
 *
 * <p><i>Fd</i>:
 * When handling a rate-event, select the time at which to estimate the
 * second derivative for the internal, continuous state model using a
 * standard finite-difference perturbation procedure.</p>
 *
 * @author David M. Lorenzetti, Contributor: Thierry S. Nouidui
 * @version $id$
 * @since Ptolemy II 10.2  // FIXME: Check version number.
 * @Pt.ProposedRating red (dmlorenzetti)
 * @Pt.AcceptedRating red (reviewmoderator)  // FIXME: Fill in.
 */
public final class QSS3Fd
    extends QSSBase {


    ///////////////////////////////////////////////////////////////////
    ////                         public methods


    /** 
     * Get the order of the external, quantized state models exposed by the integrator.
     */
    public final int getStateModelOrder() {
        return( 2 );
    }

    /** 
     * Initialize object fields (QSS-specific).
     */
    public final void initializeWorker() {

        // Check internal consistency.
        assert( _stateVals_xx == null );
        assert( _stateCt > 0 );
        assert( _ivCt >= 0 );

        // Allocate scratch memory.
        _stateVals_xx = new double[_stateCt];
        _stateDerivs_xx = new double[_stateCt];
        _stateDerivsSample_xx = new double[_stateCt];
        _rtDerivs_xx = new double[_stateCt];
        if( _ivCt > 0 ) {
            _ivVals_xx = new double[_ivCt];
        }

    }  

    ///////////////////////////////////////////////////////////////////
    ////                         protected methods

    /** 
     *  Get the predicted quantization-event time for a state (QSS-specific).
     *  
     *  @param stateIdx The state index.
     *  @param quantEvtTimeMax The maximum quantization event time.     
     */
    protected final Time _predictQuantizationEventTimeWorker(
        final int stateIdx, final Time quantEvtTimeMax) {

        // Note the superclass takes care of updating status variables and
        // storing the returned result.

        // Initialize.
        final ModelPolynomial qStateMdl = _qStateMdls[stateIdx];
        final ModelPolynomial cStateMdl = _cStateMdls[stateIdx];
        final double dq = _dqs[stateIdx];

        // Check internal consistency.
        assert( dq > 0 );
        assert( quantEvtTimeMax.getDoubleValue() > 0 );
        assert( quantEvtTimeMax.compareTo(qStateMdl.tMdl) > 0 );
        assert( quantEvtTimeMax.compareTo(cStateMdl.tMdl) > 0 );

        // Find predicted quantization-event time, as change from {tMostRecent}.
        Time tMostRecent;
        double dt;
        if( qStateMdl.tMdl.compareTo(cStateMdl.tMdl) > 0 ) {
            // Here, most recent event was a quantization-event.
            tMostRecent = qStateMdl.tMdl;
            dt = _predictQuantizationEventDeltaTimeQSS3QFromC(qStateMdl, cStateMdl, dq);
        } else {
            // Here, most recent event was a rate-event.
            tMostRecent = cStateMdl.tMdl;
            dt = _predictQuantizationEventDeltaTimeQSS3General(qStateMdl, cStateMdl, dq);
        }

        // Require {dt} > 0.
        if( dt <= 0 ) {
            // In exact arithmetic, and if the integrator is being stepped properly,
            // this should never happen.  However, if the integrator stepped to a
            // time very close to the previous predicted quantization-event time,
            // or given a small numerator and large denominator in expressions
            // above, can get nonpositive {dt}.
            //   Reset to as small a value as can manage.
            //   Use the `ulp`, the "units in the last place".  From the
            // documentation at {http://docs.oracle.com/javase/7/docs/api/java/lang/Math.html}:
            // "For a given floating-point format, an ulp of a specific real
            // number value is the distance between the two floating-point
            // values bracketing that numerical value."
            // TODO: Construct integrator with "min time step" parameter,
            // and pass it in for use it here.
            dt = java.lang.Math.ulp(tMostRecent.getDoubleValue());
        }

        // Bound result to reasonable limits.
        //   At lower end, need a positive number that, when added to {tMostRecent},
        // produces a distinct time.
        //   At upper end, can't be larger than {quantEvtTimeMax}.
        Time predQuantEvtTime;
        while( true ) {
            if( quantEvtTimeMax.subtractToDouble(tMostRecent) <= dt ) {
                // Here, tMostRecent + dt >= quantEvtTimeMax.
                //   Note determined this case in a slightly roundabout way, since
                // simply adding {dt} to {tMostRecent} may cause problems if {quantEvtTimeMax}
                // reflects some inherent limitation of class {Time}.
                predQuantEvtTime = quantEvtTimeMax;
                break;
            }
            // Here, tMostRecent + dt < quantEvtTimeMax.
            predQuantEvtTime = tMostRecent.addUnchecked(dt);
            if( predQuantEvtTime.compareTo(tMostRecent) > 0 ) {
                // Here, added {dt} and got a distinct, greater, time.
                break;
            }
            // Here, {dt} so small that can't resolve difference from {tMostRecent}.
            dt *= 2;
        }

        return( predQuantEvtTime );

    } 

    /** 
     *  Form a new external, quantized state model (QSS-specific).
     * 
     *  @param stateIdx The state index.
     */
    protected final void _triggerQuantizationEventWorker(final int stateIdx) {

        // Note the superclass takes care of updating status variables and so on.

        // Initialize.
        final ModelPolynomial qStateMdl = _qStateMdls[stateIdx];
        final ModelPolynomial cStateMdl = _cStateMdls[stateIdx];
        final double dtStateMdl = _currSimTime.subtractToDouble(cStateMdl.tMdl);

        // Update the external, quantized state model.
        qStateMdl.tMdl = _currSimTime;
        qStateMdl.coeffs[0] = cStateMdl.evaluate(dtStateMdl);
        qStateMdl.coeffs[1] = cStateMdl.evaluateDerivative(dtStateMdl);
        qStateMdl.coeffs[2] = cStateMdl.evaluateDerivative2(dtStateMdl) / 2;

    }  


    /** 
     * Form new internal, continuous state models (QSS-specific).
     */
    protected final void _triggerRateEventWorker()
        throws Exception {

        // Note the superclass takes care of updating status variables and so on.

        // Get values, at {_currSimTime}, of arguments to derivative function.
        //   In general, expect the integrator formed all of its
        // continuous state models at the same time.  If so, can find a
        // single delta-time, rather than having to find multiple differences
        // from {_currSimTime}.  Know that finding time differences is
        // expensive in Ptolemy, so want to avoid doing that if possible.
        //   However, there is a chance that the continuous state models were
        // formed at different times.  For example:
        // (1) User can reset a single state at any simulation time.
        // (2) In future, might be possible to avoid updating a
        // continuous state model if know none of its arguments changed.
        Time tStateMdl = null;
        double dtStateMdl = 0;
        for( int ii=0; ii<_stateCt; ++ii ) {
            final ModelPolynomial cStateMdl = _cStateMdls[ii];
            // Check for different model time.  Note testing object identity OK.
            if( cStateMdl.tMdl != tStateMdl ) {
                tStateMdl = cStateMdl.tMdl;
                dtStateMdl = _currSimTime.subtractToDouble(tStateMdl);
            }
            _stateVals_xx[ii] = cStateMdl.evaluate(dtStateMdl);
        }
        // In general, don't expect input variable models to have same times.
        for( int ii=0; ii<_ivCt; ++ii ) {
            _ivVals_xx[ii] = _ivMdls[ii].evaluate(_currSimTime);
        }

        // Evaluate derivative function at {_currSimTime}.
        int retVal = _derivFcn.evaluateDerivatives(_currSimTime, _stateVals_xx, _ivVals_xx,
            _stateDerivs_xx);
        if( 0 != retVal ) {
            throw new Exception("_derivFcn.evalDerivs() returned " +retVal);
        }

        // Update the internal, continuous state models.
        //   Note this is a partial update, since don't yet know the second
        // derivatives.  Do the update now so that, when find the sample point
        // needed to estimate second derivatives, will be able to use as much
        // current information about the continuous state as possible.
        //   This also updates the rate model, which is just the derivative of
        // the state model.
        for( int ii=0; ii<_stateCt; ++ii ) {
            final ModelPolynomial cStateMdl = _cStateMdls[ii];
            cStateMdl.tMdl = _currSimTime;
            cStateMdl.coeffs[0] = _stateVals_xx[ii];
            cStateMdl.coeffs[1] = _stateDerivs_xx[ii];
            cStateMdl.coeffs[2] = 0;
            cStateMdl.coeffs[3] = 0;
        }

        // Choose a sample time, different from {_currSimTime}.
        //   For estimating second derivatives.
        final double dtSample = 1e-8 * Math.max(1, Math.abs(_currSimTime.getDoubleValue()));
        final Time tSample = _currSimTime.addUnchecked(dtSample);

        // Get values, at {tSample}, of arguments to derivative function.
        //   Note that here, know all continous state models have same time.
        // Therefore can use same delta-time for all evals.
        for( int ii=0; ii<_stateCt; ++ii ) {
            _stateVals_xx[ii] = _cStateMdls[ii].evaluate(dtSample);
        }
        for( int ii=0; ii<_ivCt; ++ii ) {
            _ivVals_xx[ii] = _ivMdls[ii].evaluate(tSample);
        }

        // Evaluate derivative function at {tSample}.
        retVal = _derivFcn.evaluateDerivatives(tSample, _stateVals_xx, _ivVals_xx,
            _stateDerivsSample_xx);
        if( 0 != retVal ) {
            throw new Exception("_derivFcn.evalDerivs() returned " +retVal);
        }

        // Update the internal, continuous state models.
        final double oneOverDtSample = 1.0 / dtSample;
        for( int ii=0; ii<_stateCt; ++ii ) {
            final double rtDeriv = oneOverDtSample * (_stateDerivsSample_xx[ii] - _stateDerivs_xx[ii]);
            _rtDerivs_xx[ii] = rtDeriv;
            _cStateMdls[ii].coeffs[2] = 0.5*rtDeriv;
        }

        // Choose a sample time, different from {_currSimTime} and different from {tSample}.
        //   For estimating third derivatives.
        final double dtSample2 = Math.sqrt(dtSample);
        final Time tSample2 = _currSimTime.addUnchecked(dtSample2);

        // Get values, at {tSample2}, of arguments to derivative function.
        //   Note that here, know all continous state models have same time.
        // Therefore can use same delta-time for all evals.
        for( int ii=0; ii<_stateCt; ++ii ) {
            _stateVals_xx[ii] = _cStateMdls[ii].evaluate(dtSample2);
        }
        for( int ii=0; ii<_ivCt; ++ii ) {
            _ivVals_xx[ii] = _ivMdls[ii].evaluate(tSample2);
        }

        // Evaluate derivative function at {tSample2}.
        retVal = _derivFcn.evaluateDerivatives(tSample2, _stateVals_xx, _ivVals_xx,
            _stateDerivsSample_xx);
        if( 0 != retVal ) {
            throw new Exception("_derivFcn.evalDerivs() returned " +retVal);
        }

        // Update the internal, continuous state models.
        final double oneOverThreeDtSampleSq = 1.0 / (3*dtSample2*dtSample2);
        for( int ii=0; ii<_stateCt; ++ii ) {
            _cStateMdls[ii].coeffs[3] = oneOverThreeDtSampleSq *
                (_stateDerivsSample_xx[ii] - _stateDerivs_xx[ii] - _rtDerivs_xx[ii]*dtSample2);
        }

    } 

    ///////////////////////////////////////////////////////////////////
    ////                         private variables

    // Scratch memory.
    //   For local (per-method) calculations, not inter-method communication.
    // When a method returns, it should be possible to write random values to
    // this memory, without losing any information about the solver state.
    //   Thus, if the integrator got serialized (marshalled) to disk, this
    // memory could be ignored.
    private double[] _stateVals_xx;
    private double[] _stateDerivs_xx;
    private double[] _stateDerivsSample_xx;
    private double[] _rtDerivs_xx;
    private double[] _ivVals_xx;


}  
