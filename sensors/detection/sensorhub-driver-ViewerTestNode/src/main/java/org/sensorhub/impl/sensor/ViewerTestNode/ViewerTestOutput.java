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

import net.opengis.swe.v20.*;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.swe.SWEBuilders;
import org.vast.swe.SWEHelper;
import org.vast.swe.helper.GeoPosHelper;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;


public class ViewerTestOutput extends AbstractSensorOutput<ViewerTestSensor> {
    static final String SENSOR_OUTPUT_NAME = "ViewerTestOutput";
    static final String SENSOR_OUTPUT_LABEL = "Test Data";
    static final String SENSOR_OUTPUT_DESCRIPTION = "Sample Data to test the Viewer";

    // myNote:
    // Added Variables because there were in other templates
    private static final int MAX_NUM_TIMING_SAMPLES = 10;
    private int setCount = 0;
    private final long[] timingHistogram = new long[MAX_NUM_TIMING_SAMPLES];
    private final Object histogramLock = new Object();
    private long lastSetTimeMillis = System.currentTimeMillis();

    private static final Logger logger = LoggerFactory.getLogger( ViewerTestOutput.class);

    private DataRecord dataStruct;
    private DataEncoding dataEncoding;

    /**
     * Creates a new output for the sensor driver.
     *
     * @param parentSensor Sensor driver providing this output.
     */
    public ViewerTestOutput(ViewerTestSensor parentSensor) {
        super(SENSOR_OUTPUT_NAME, parentSensor);
    }

    /**
     * Initializes the data structure for the output, defining the fields, their ordering, and data types.
     */
    public void doInit() {
        logger.info("Initializing ViewerTest-Output");
        // Get an instance of SWE Factory suitable to build components
        SWEHelper sweFactory = new SWEHelper();
        GeoPosHelper geoFac = new GeoPosHelper();

        // Create the data record description
        SWEBuilders.DataRecordBuilder recordBuilder = sweFactory.createRecord()
                .name(SENSOR_OUTPUT_NAME)
                .label(SENSOR_OUTPUT_LABEL)
                .description(SENSOR_OUTPUT_DESCRIPTION)
                .definition(SWEHelper.getPropertyUri("Output"))
                .addField("time", sweFactory.createTime()
                        .asSamplingTimeIsoUTC()
                        .label("Collection Time")
                        .description("Timestamp for when reading was generated")
                        .definition(SWEHelper.getPropertyUri("time")))
                .addField("raw_lob", sweFactory.createQuantity()
                        .uomCode("deg")
                        .label("Raw LOB")
                        .description("The LOB to the emitter in absolute (true north) value")
                        .definition(SWEHelper.getPropertyUri("lob")))
                .addField("location", geoFac.createLocationVectorLatLon().label(SWEHelper.getPropertyUri("location"))
                        .label("Location")
                        .description("Genereated Lat and Long for the Sensor's Position")
                )
                ;
        dataStruct = recordBuilder.build();


        dataEncoding = sweFactory.newTextEncoding(",", "\n");
    }

    @Override
    public DataComponent getRecordDescription() {
        return dataStruct;
    }

    @Override
    public DataEncoding getRecommendedEncoding() {
        return dataEncoding;
    }

    @Override
    public double getAverageSamplingPeriod() {
        long accumulator = 0;
        synchronized (histogramLock) {
            for (int idx = 0; idx < MAX_NUM_TIMING_SAMPLES; ++idx) {
                accumulator += timingHistogram[idx];
            }
        }
        return accumulator / (double) MAX_NUM_TIMING_SAMPLES;
    }

    public void SetData() {
        DataBlock dataBlock;
        try {
            if (latestRecord == null) {
                dataBlock = dataStruct.createDataBlock();
            } else {
                dataBlock = latestRecord.renew();
            }
            synchronized (histogramLock) {
                int setIndex = setCount % MAX_NUM_TIMING_SAMPLES;
                // Get a sampling time for latest set based on previous set sampling time
                timingHistogram[setIndex] = System.currentTimeMillis() - lastSetTimeMillis;
                // Set latest sampling time to now
                lastSetTimeMillis = timingHistogram[setIndex];
            }
            ++setCount;
            // Create a random Time
            long epochSeconds = System.currentTimeMillis() / 1000;

            OffsetDateTime odt = OffsetDateTime.ofInstant(
                    Instant.ofEpochSecond(epochSeconds),
                    ZoneOffset.UTC
            );


            // Create a random degree between 0-360
            double rawLOB = Math.random() * 360;

            // Create a position (botts inc)
            double lat = 34.666034;
            double lon = -86.780279;


            dataBlock.setDateTime(0, odt);   // time
            dataBlock.setDoubleValue(1, rawLOB);  // Line of Bearing (DoA)
            dataBlock.setDoubleValue(2, lat);  // location lat
            dataBlock.setDoubleValue(3, lon);  // location lon

            latestRecord = dataBlock;
            latestRecordTime = System.currentTimeMillis();

            eventHandler.publish(new DataEvent(latestRecordTime, ViewerTestOutput.this, dataBlock));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

}
