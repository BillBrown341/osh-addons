/***************************** BEGIN LICENSE BLOCK ***************************
 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2020-2025 Botts Innovative Research, Inc. All Rights Reserved.
 ******************************* END LICENSE BLOCK ***************************/
package org.sensorhub.impl.sensor.ViewerTestNode;

import com.google.gson.JsonObject;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.*;
import java.util.concurrent.TimeUnit;

/**
 * Driver implementation for the sensor.
 * <p>
 * This class is responsible for providing sensor information, managing output registration,
 * and performing initialization and shutdown for the driver and its outputs.
 */
public class ViewerTestSensor extends AbstractSensorModule<ViewerTestConfig> implements Runnable {
    static final String UID_PREFIX = "osh:viewerTestNode:";
    static final String XML_PREFIX = "viewerTestNode";

    private static final Logger logger = LoggerFactory.getLogger(ViewerTestSensor.class);

    /// GLOBAL VARIABLES FOR SENSOR OPERATION
    ViewerTestOutput viewerTestOutput;

    private volatile boolean keepRunning = false;

    ///  INITIALIZE
    @Override
    public void doInit() throws SensorHubException {
        super.doInit();

        // Create SensorHub Identifiers using designated prefix and serial number from Admin Panel
        generateUniqueID(UID_PREFIX, config.serialNumber);
        generateXmlID(XML_PREFIX, config.serialNumber);

        /// INITIALIZE OUTPUTS
        viewerTestOutput = new ViewerTestOutput(this);
        addOutput(viewerTestOutput, false);
        viewerTestOutput.doInit();

    }


    @Override
    public void doStart() throws SensorHubException {
        super.doStart();

        // Set variable to continue readings
        keepRunning = true;

        // CREATE THREAD THAT CONTINUALLY READS SENSOR REPORT
        Thread runSensor = new Thread(this, "Sensor Worker");
        runSensor.start();    // This starts the the run() method

    }

    @Override
    public void doStop() throws SensorHubException {
        keepRunning = false;
        super.doStop();
    }

    @Override
    public boolean isConnected() {
        return true;
    }


    @Override
    public void run() {
        while (keepRunning) {
            // Send a GET request to the DoA URL
            try {

                viewerTestOutput.SetData();
                Thread.sleep(2000); // Sleep for 2 seconds

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }
}
