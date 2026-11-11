package com.gregisoft.explorer.cli.garmin;

import com.garmin.fit.Decode;
import com.garmin.fit.FileIdMesg;
import com.garmin.fit.FileIdMesgListener;
import com.garmin.fit.MesgBroadcaster;
import com.garmin.fit.RecordMesg;
import com.garmin.fit.RecordMesgListener;
import com.garmin.fit.SessionMesg;
import com.garmin.fit.SessionMesgListener;
import com.garmin.fit.Sport;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class FitToGpxConverter {

    private static final double SEMICIRCLES_TO_DEGREES = 180.0 / 2147483648.0;

    public void convert(byte[] fitBytes, Path targetGpx) {
        DecodedActivity activity = decode(fitBytes);
        writeGpx(activity.points(), targetGpx);
    }

    DecodedActivity decode(byte[] fitBytes) {
        if (fitBytes == null || fitBytes.length == 0) {
            throw new FitConversionException("FIT activity is empty.");
        }
        Decode integrityDecoder = new Decode();
        try {
            if (!integrityDecoder.checkFileIntegrity(new ByteArrayInputStream(fitBytes))) {
                throw new FitConversionException("FIT activity is corrupt or truncated.");
            }

            Decode decoder = new Decode();
            MesgBroadcaster broadcaster = new MesgBroadcaster(decoder);
            Collector collector = new Collector();
            broadcaster.addListener((FileIdMesgListener) collector);
            broadcaster.addListener((SessionMesgListener) collector);
            broadcaster.addListener((RecordMesgListener) collector);
            boolean decoded = decoder.read(
                    new ByteArrayInputStream(fitBytes), broadcaster, broadcaster);
            if (!decoded) {
                throw new FitConversionException("FIT activity could not be decoded completely.");
            }
            return collector.result();
        } catch (FitConversionException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new FitConversionException("FIT activity could not be decoded.", exception);
        }
    }

    private static void writeGpx(List<FitPoint> points, Path targetGpx) {
        XMLOutputFactory factory = XMLOutputFactory.newFactory();
        try (OutputStream output = Files.newOutputStream(targetGpx)) {
            XMLStreamWriter xml = factory.createXMLStreamWriter(output, "UTF-8");
            try {
                xml.writeStartDocument("UTF-8", "1.0");
                xml.writeStartElement("gpx");
                xml.writeDefaultNamespace("http://www.topografix.com/GPX/1/1");
                xml.writeAttribute("version", "1.1");
                xml.writeAttribute("creator", "Explorer Platform Garmin import");
                xml.writeStartElement("trk");
                xml.writeStartElement("trkseg");
                for (FitPoint point : points) {
                    xml.writeStartElement("trkpt");
                    xml.writeAttribute("lat", Double.toString(point.latitude()));
                    xml.writeAttribute("lon", Double.toString(point.longitude()));
                    if (point.elevationMeters() != null) {
                        xml.writeStartElement("ele");
                        xml.writeCharacters(Double.toString(point.elevationMeters()));
                        xml.writeEndElement();
                    }
                    if (point.timestamp() != null) {
                        xml.writeStartElement("time");
                        xml.writeCharacters(point.timestamp().toString());
                        xml.writeEndElement();
                    }
                    xml.writeEndElement();
                }
                xml.writeEndElement();
                xml.writeEndElement();
                xml.writeEndElement();
                xml.writeEndDocument();
                xml.flush();
            } finally {
                xml.close();
            }
        } catch (IOException | XMLStreamException exception) {
            throw new FitConversionException("Could not create the temporary GPX file.", exception);
        }
    }

    record DecodedActivity(List<FitPoint> points) {
        DecodedActivity {
            points = List.copyOf(points);
        }
    }

    record FitPoint(
            double latitude,
            double longitude,
            Double elevationMeters,
            Instant timestamp
    ) {
    }

    private static final class Collector
            implements FileIdMesgListener, SessionMesgListener, RecordMesgListener {

        private final List<FitPoint> points = new ArrayList<>();
        private com.garmin.fit.File fileType;
        private int sessions;
        private boolean multisport;
        private Instant previousTimestamp;
        private int elevationCount;

        @Override
        public void onMesg(FileIdMesg message) {
            fileType = message.getType();
        }

        @Override
        public void onMesg(SessionMesg message) {
            sessions++;
            multisport |= message.getSport() == Sport.MULTISPORT;
        }

        @Override
        public void onMesg(RecordMesg message) {
            Integer latitude = message.getPositionLat();
            Integer longitude = message.getPositionLong();
            if (latitude == null || longitude == null) {
                return;
            }
            double latitudeDegrees = latitude * SEMICIRCLES_TO_DEGREES;
            double longitudeDegrees = longitude * SEMICIRCLES_TO_DEGREES;
            if (!Double.isFinite(latitudeDegrees) || latitudeDegrees < -90 || latitudeDegrees > 90
                    || !Double.isFinite(longitudeDegrees)
                    || longitudeDegrees < -180 || longitudeDegrees > 180) {
                throw new FitConversionException("FIT activity contains invalid GPS coordinates.");
            }
            Float enhancedAltitude = message.getEnhancedAltitude();
            Float altitude = enhancedAltitude != null ? enhancedAltitude : message.getAltitude();
            Double elevation = altitude == null ? null : altitude.doubleValue();
            if (elevation != null) {
                if (!Double.isFinite(elevation)) {
                    throw new FitConversionException("FIT activity contains invalid elevation data.");
                }
                elevationCount++;
            }
            Instant timestamp = message.getTimestamp() == null
                    ? null : message.getTimestamp().getInstant();
            if (timestamp != null && previousTimestamp != null && timestamp.isBefore(previousTimestamp)) {
                throw new FitConversionException("FIT activity records are not in chronological order.");
            }
            if (timestamp != null) {
                previousTimestamp = timestamp;
            }
            points.add(new FitPoint(latitudeDegrees, longitudeDegrees, elevation, timestamp));
        }

        private DecodedActivity result() {
            if (fileType != com.garmin.fit.File.ACTIVITY) {
                throw new FitConversionException("The downloaded FIT file is not an Activity file.");
            }
            if (sessions != 1 || multisport) {
                throw new FitConversionException(
                        "Explorer imports exactly one non-multisport FIT session.");
            }
            if (points.size() < 2) {
                throw new FitConversionException(
                        "FIT activity does not contain at least two usable GPS records.");
            }
            if (elevationCount < 2) {
                throw new FitConversionException(
                        "FIT activity does not contain enough elevation records for Explorer.");
            }
            return new DecodedActivity(points);
        }
    }
}
