/*
@Copyright (c) 2010 The Regents of the University of California.
All rights reserved.

Permission is hereby granted, without written agreement and without
license or royalty fees, to use, copy, modify, and distribute this
software and its documentation for any purpose, provided that the
above copyright notice and the following two paragraphs appear in all
copies of this software.

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
/*
 * 
 */
package ptdb.gui;

import java.io.IOException;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import ptdb.common.dto.SetupParameters;
import ptdb.common.exception.DBConnectionException;
import ptdb.kernel.bl.setup.SetupManager;

///////////////////////////////////////////////////////////////
//// DatabaseSetupFrame

/**
 * Database setup frame to modify and test the datadase connection.
 * 
 * @author Ashwini Bijwe
 * @version $Id$
 * @since Ptolemy II 8.1
 * @Pt.ProposedRating Red (abijwe)
 * @Pt.AcceptedRating Red (abijwe)
 *
 */
public class DatabaseSetupFrame extends JFrame {

    /** Create a new Database setup frame with values 
     * from the properties file. */
    public DatabaseSetupFrame() {
        this.setTitle("Setup Database Connection");
        initComponents();
        _getSetupParameters();
        _populateSetupParameters();
    }

    //////////////////////////////////////////////////////////////////////
    ////		public variables 				////

    //////////////////////////////////////////////////////////////////////
    ////		public methods 					////

    //////////////////////////////////////////////////////////////////////
    ////		protected methods 				////

    //////////////////////////////////////////////////////////////////////
    ////		protected variables 				////

    //////////////////////////////////////////////////////////////////////
    ////		private methods 				////
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        _urlTextField = new javax.swing.JTextField();
        _containerNameTextField = new javax.swing.JTextField();
        urlLabel = new javax.swing.JLabel();
        containerLabel = new javax.swing.JLabel();
        testConnectionButton = new javax.swing.JButton();
        _cacheContainerNameTextField = new javax.swing.JTextField();
        cacheLabel = new javax.swing.JLabel();
        saveButton = new javax.swing.JButton();
        resetButton = new javax.swing.JButton();
        _cancelButton = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        urlLabel.setText("URL");

        containerLabel.setText("Database Container Name");

        testConnectionButton.setText("Test Connection");
        testConnectionButton
                .addActionListener(new java.awt.event.ActionListener() {
                    public void actionPerformed(java.awt.event.ActionEvent evt) {
                        testConnectionButtonActionPerformed(evt);
                    }
                });

        cacheLabel.setText("Cache Container Name");

        saveButton.setText("Save");
        saveButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                _saveButtonActionPerformed(evt);
            }
        });

        resetButton.setText("Reset");
        resetButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                resetButtonActionPerformed(evt);
            }
        });

        _cancelButton.setText("Cancel");
        _cancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(
                jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout
                .setHorizontalGroup(jPanel1Layout
                        .createParallelGroup(
                                javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(
                                jPanel1Layout
                                        .createSequentialGroup()
                                        .addGroup(
                                                jPanel1Layout
                                                        .createParallelGroup(
                                                                javax.swing.GroupLayout.Alignment.LEADING)
                                                        .addGroup(
                                                                jPanel1Layout
                                                                        .createSequentialGroup()
                                                                        .addContainerGap()
                                                                        .addGroup(
                                                                                jPanel1Layout
                                                                                        .createParallelGroup(
                                                                                                javax.swing.GroupLayout.Alignment.TRAILING)
                                                                                        .addComponent(
                                                                                                containerLabel)
                                                                                        .addComponent(
                                                                                                urlLabel)
                                                                                        .addComponent(
                                                                                                cacheLabel))
                                                                        .addGap(
                                                                                33,
                                                                                33,
                                                                                33)
                                                                        .addGroup(
                                                                                jPanel1Layout
                                                                                        .createParallelGroup(
                                                                                                javax.swing.GroupLayout.Alignment.TRAILING,
                                                                                                false)
                                                                                        .addComponent(
                                                                                                _containerNameTextField,
                                                                                                javax.swing.GroupLayout.Alignment.LEADING)
                                                                                        .addComponent(
                                                                                                _urlTextField,
                                                                                                javax.swing.GroupLayout.Alignment.LEADING,
                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                281,
                                                                                                Short.MAX_VALUE)
                                                                                        .addComponent(
                                                                                                _cacheContainerNameTextField,
                                                                                                javax.swing.GroupLayout.Alignment.LEADING)))
                                                        .addGroup(
                                                                jPanel1Layout
                                                                        .createSequentialGroup()
                                                                        .addGap(
                                                                                128,
                                                                                128,
                                                                                128)
                                                                        .addComponent(
                                                                                testConnectionButton)
                                                                        .addPreferredGap(
                                                                                javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                        .addComponent(
                                                                                saveButton)
                                                                        .addPreferredGap(
                                                                                javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                        .addComponent(
                                                                                resetButton)
                                                                        .addPreferredGap(
                                                                                javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                        .addComponent(
                                                                                _cancelButton)))
                                        .addContainerGap(22, Short.MAX_VALUE)));
        jPanel1Layout
                .setVerticalGroup(jPanel1Layout
                        .createParallelGroup(
                                javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(
                                jPanel1Layout
                                        .createSequentialGroup()
                                        .addGap(24, 24, 24)
                                        .addGroup(
                                                jPanel1Layout
                                                        .createParallelGroup(
                                                                javax.swing.GroupLayout.Alignment.BASELINE)
                                                        .addComponent(
                                                                _urlTextField,
                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                        .addComponent(urlLabel))
                                        .addPreferredGap(
                                                javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addGroup(
                                                jPanel1Layout
                                                        .createParallelGroup(
                                                                javax.swing.GroupLayout.Alignment.TRAILING)
                                                        .addComponent(
                                                                _containerNameTextField,
                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                        .addComponent(
                                                                containerLabel))
                                        .addPreferredGap(
                                                javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addGroup(
                                                jPanel1Layout
                                                        .createParallelGroup(
                                                                javax.swing.GroupLayout.Alignment.TRAILING)
                                                        .addComponent(
                                                                _cacheContainerNameTextField,
                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                        .addComponent(
                                                                cacheLabel))
                                        .addGap(28, 28, 28)
                                        .addGroup(
                                                jPanel1Layout
                                                        .createParallelGroup(
                                                                javax.swing.GroupLayout.Alignment.BASELINE)
                                                        .addComponent(
                                                                saveButton)
                                                        .addComponent(
                                                                resetButton)
                                                        .addComponent(
                                                                _cancelButton)
                                                        .addComponent(
                                                                testConnectionButton))
                                        .addContainerGap(12, Short.MAX_VALUE)));

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(
                getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(layout.createParallelGroup(
                javax.swing.GroupLayout.Alignment.LEADING).addGroup(
                javax.swing.GroupLayout.Alignment.TRAILING,
                layout.createSequentialGroup().addContainerGap(
                        javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jPanel1,
                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                javax.swing.GroupLayout.PREFERRED_SIZE).addGap(
                                69, 69, 69)));
        layout.setVerticalGroup(layout.createParallelGroup(
                javax.swing.GroupLayout.Alignment.LEADING).addGroup(
                layout.createSequentialGroup().addComponent(jPanel1,
                        javax.swing.GroupLayout.PREFERRED_SIZE,
                        javax.swing.GroupLayout.DEFAULT_SIZE,
                        javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE,
                                Short.MAX_VALUE)));

        pack();
    }// </editor-fold>

    /**
     * Take action for the connection button event.
     * @param evt Connection button event.
     */
    private void testConnectionButtonActionPerformed(
            java.awt.event.ActionEvent evt) {
        _testConnection();
    }

    /**
     * Take action for the save button event.
     * @param evt Save button event.
     */
    private void _saveButtonActionPerformed(java.awt.event.ActionEvent evt) {
        _saveSetupParameters();
    }

    /**
     * Take action for the reset button event.
     * @param evt Reset button event.
     */
    private void resetButtonActionPerformed(java.awt.event.ActionEvent evt) {
        _resetSetupParameters();
    }

    /**
     * Take action for the cancel button event.
     * @param evt Cancel button event.
     */
    private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt) {
        this.setVisible(false);
        dispose();
    }

    /**
    * @param args the command line arguments
    */
    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new DatabaseSetupFrame().setVisible(true);
            }
        });
    }

    /**
     * Get the setup parameters to populate form fields.
     */
    private void _getSetupParameters() {
        SetupManager setupManager = new SetupManager();
        _setupParameters = setupManager.getSetupParameters();
    }

    /**
     * Populate the three text fields on the form. 
     */
    private void _populateSetupParameters() {

        _cacheContainerNameTextField.setText(_setupParameters
                .getCacheContainerName());
        _containerNameTextField.setText(_setupParameters.getContainerName());
        _urlTextField.setText(_setupParameters.getUrl());
    }

    /**
     * Validate the text fields on the form for null or blank values.
     */
    private boolean _validateSetupParameters() {
        if (_urlTextField.getText() == null
                || _urlTextField.getText().trim().length() == 0) {
            JOptionPane.showMessageDialog(DatabaseSetupFrame.this,
                    "Please specify the URL.");
            _urlTextField.requestFocusInWindow();
            return false;
        }

        if (_containerNameTextField.getText() == null
                || _containerNameTextField.getText().trim().length() == 0) {
            JOptionPane.showMessageDialog(DatabaseSetupFrame.this,
                    "Please specify the Database Container Name.");
            _containerNameTextField.requestFocusInWindow();
            return false;
        }

        if (_cacheContainerNameTextField.getText() == null
                || _cacheContainerNameTextField.getText().trim().length() == 0) {
            JOptionPane.showMessageDialog(DatabaseSetupFrame.this,
                    "Please specify the Cache Container Name.");
            _cacheContainerNameTextField.requestFocusInWindow();
            return false;
        }
        return true;
    }

    /**
     * Save the fields to the properties file.
     */
    private void _saveSetupParameters() {
        SetupManager setupManager = new SetupManager();

        boolean isValid = _validateSetupParameters();

        if (isValid) {
            SetupParameters setupParameters = _readSetupParameters();
            try {
                try{
                setupManager.updateDBConnectionSetupParameters(setupParameters);
                } catch (ExceptionInInitializerError e) {
                    
                }
                JOptionPane.showMessageDialog(DatabaseSetupFrame.this,
                        "Setup parameters have been saved.");
                _setupParameters = setupParameters;
            } catch (DBConnectionException e) {
                JOptionPane.showMessageDialog(DatabaseSetupFrame.this,
                        "Error while connecting to the new connection - "
                                + e.getMessage(), "Connection Error",
                        JOptionPane.INFORMATION_MESSAGE, null);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(DatabaseSetupFrame.this,
                        "Error while saving parameters - " + e.getMessage(),
                        "Save Error", JOptionPane.INFORMATION_MESSAGE, null);
            }
        }
    }

    /**
     * Create a test connection using the setup parameters from the form.
     */
    private void _testConnection() {
        SetupManager setupManager = new SetupManager();
        boolean isValid = _validateSetupParameters();
        if (isValid) {
            try {
                setupManager.testConnection(_readSetupParameters());
                JOptionPane.showMessageDialog(DatabaseSetupFrame.this,
                        "Connection test was successful.");
            } catch (DBConnectionException e) {
                JOptionPane.showMessageDialog(DatabaseSetupFrame.this, e
                        .getMessage(), "Connection Error",
                        JOptionPane.INFORMATION_MESSAGE, null);
            }
        }
    }

    /**
     * Read the setup parameters from the form.
     */
    private SetupParameters _readSetupParameters() {
        SetupParameters setupParameters = new SetupParameters(_urlTextField
                .getText(), _containerNameTextField.getText(),
                _cacheContainerNameTextField.getText());
        return setupParameters;
    }

    /**
     * Reset the setup parameter values to the ones in the property file.
     */
    private void _resetSetupParameters() {
        _populateSetupParameters();
    }

    ///
    ///////////////////////////////////////////////////////////////////
    ////		private variables				////
    private SetupParameters _setupParameters;
    private javax.swing.JTextField _cacheContainerNameTextField;
    private javax.swing.JButton _cancelButton;
    private javax.swing.JTextField _containerNameTextField;
    private javax.swing.JLabel urlLabel;
    private javax.swing.JLabel containerLabel;
    private javax.swing.JLabel cacheLabel;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JButton resetButton;
    private javax.swing.JButton saveButton;
    private javax.swing.JButton testConnectionButton;
    private javax.swing.JTextField _urlTextField;
}
