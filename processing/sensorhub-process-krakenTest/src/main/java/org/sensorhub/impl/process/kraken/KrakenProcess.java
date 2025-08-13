/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2021 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.process.kraken;

import net.opengis.swe.v20.*;
import net.opengis.swe.v20.Boolean;
import net.opengis.swe.v20.Text;

import org.sensorhub.api.processing.OSHProcessInfo;
import org.vast.process.ExecutableProcessImpl;
import org.vast.process.ProcessException;

import org.vast.swe.SWEHelper;
import org.vast.swe.helper.GeoPosHelper;



/**
 * <p>
 * KrakenSDR Test
 * </p>
 *
 * @author Bill Brown
 */
public class KrakenProcess extends ExecutableProcessImpl
{
	public static final OSHProcessInfo INFO = new OSHProcessInfo("krakenTest", "my process", null, KrakenProcess.class);

    // INPUT VARIABLES
    Time inputTimeStamp;
    Quantity inputRawLoB;
    Quantity inputConfidence;
    Quantity inputRSSI;
    Quantity inputFreq;
    Text inputAntennaArrangement;
    Quantity inputLatency;
    Quantity inputHeading;
    Text inputUUID;
    Text inputUsedHeading;
    Vector inputLocation;

    // OUTPUT VARIABLES
    Time outputTime;
    Boolean outputInArea;


    public KrakenProcess() {
        super(INFO);

        SWEHelper swe = new SWEHelper();
        GeoPosHelper geo = new GeoPosHelper();

        //DATA STRUCTURE
        // inputs
        inputData.add("DOA_INPUT", swe.createRecord()
            .label("DOA Input Source")
            .addField("time", inputTimeStamp = swe.createTime()
                    .asSamplingTimeIsoUTC()
                    .label("KrakenSDR Collection Time")
                    .build()
            )
            .addField("raw-lob", inputRawLoB = swe.createQuantity()
                    .id("IN_Raw_LoB")
                    .uomCode("deg")
                    .label("Input Raw LoB")
                    .build()
            )
            .addField("confidence", inputConfidence = swe.createQuantity()
                    .label("Confidence")
                    .description("Confidence Value (0-99). The higher the better")
                    .definition(SWEHelper.getPropertyUri("confidence"))
                    .build()
            )
            .addField("rssi", inputRSSI = swe.createQuantity()
                    .uomCode("dB")
                    .label("RSSI")
                    .description("Received Signal Strength Indicator value of the event")
                    .definition(SWEHelper.getPropertyUri("rssi"))
                    .build()
            )
            .addField("frequency", inputFreq = swe.createQuantity()
                    .uomCode("Hz")
                    .label("frequency")
                    .dataType(DataType.LONG)
                    .description("The transmission frequency of the event in Hertz")
                    .definition(SWEHelper.getPropertyUri("frequency"))
                    .build()
            )
            .addField("antenna_arrangement", inputAntennaArrangement = swe.createText()
                    .label("Antenna Arrangement")
                    .description("Antenna Array Arrangement : (\"UCA\"/\"ULA\"/\"Custom\")")
                    .definition(SWEHelper.getPropertyUri("antenna_arrangement"))
                    .build()
            )
            .addField("time-delta", inputLatency = swe.createQuantity()
                    .uomCode("ms")
                    .label("latency")
                    .description("Latency in ms : (Time from signal arrival at antenna, to result. NOT including network latency.)")
                    .definition(SWEHelper.getPropertyUri("latency"))
                    .build()
            )
            .addField("uuid", inputUUID = swe.createText()
                    .label("UUID")
                    .description("Name of the KrakenSDR station inputted in the Station Information box in the Web GUI")
                    .definition(SWEHelper.getPropertyUri("id"))
                    .build()
            )
            .addField("location", inputLocation = geo.newLocationVectorLatLon(SWEHelper.getPropertyUri("location")))
            .addField("heading", inputHeading = swe.createQuantity()
                    .label("heading")
                    .uomCode("deg")
                    .description("heading")
                    .definition(SWEHelper.getPropertyUri("heading"))
                    .build()
            )
            .addField("used_heading", inputUsedHeading = swe.createText()
                    .label("Main Heading Sensor Used")
                    .description("Main Heading Sensor Used (\"GPS\"/\"Compass\")")
                    .definition(SWEHelper.getPropertyUri("heading"))
                    .build()
            )
            .build());

        // NO PARAMETERS
        // OUTPUT DATA

        outputInArea = swe.createBoolean()
                .id("OUT_inArea")
                .label("Output Boolean if in area or not")
                .build();


        outputData.add("outputInArea",  outputInArea);


    }


    @Override
    public void execute() throws ProcessException
    {
        double inputValue = inputRawLoB.getValue();
        System.out.println(inputValue);
        outputInArea.getData().setBooleanValue(inputValue > 270 && inputValue < 360);
    }

}