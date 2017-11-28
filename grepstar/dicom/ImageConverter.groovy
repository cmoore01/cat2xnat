package grepstar.dicom

import org.dcm4che3.data.Attributes
import org.dcm4che3.data.ElementDictionary
import org.dcm4che3.data.Tag
import org.dcm4che3.data.UID
import org.dcm4che3.tool.dcm2dcm.Dcm2Dcm
import org.dcm4che3.tool.jpg2dcm.Jpg2Dcm
import org.dcm4che3.util.UIDUtils
import org.joda.time.LocalDateTime

class ImageConverter {

    private final ElementDictionary elementDictionary = ElementDictionary.getStandardElementDictionary()

    void convert(List<Image> images, String name, String modality) {
        final Attributes attributes = new Attributes()
        final LocalDateTime now = LocalDateTime.now()
        setHeader(attributes, Tag.Modality, modality)
        String sopClassUid
        switch (modality) {
            case 'MR':
                sopClassUid = UID.MRImageStorage
                break
            case 'PT':
                sopClassUid = UID.PositronEmissionTomographyImageStorage
                break
            case 'CT':
                sopClassUid = UID.CTImageStorage
                break
            default:
                sopClassUid = UID.SecondaryCaptureImageStorage
        }
        setHeader(attributes, Tag.SOPClassUID, sopClassUid)
        setHeader(attributes, Tag.StudyInstanceUID, UIDUtils.createUID())
        attributes.setDate(Tag.ContentDateAndTime, now.toDate())
        attributes.setDate(Tag.InstanceCreationDateAndTime, now.toDate())
        setHeader(attributes, Tag.StudyDate, now.toString('YYYYMMdd'))
        setHeader(attributes, Tag.StudyTime, now.toString('HHmmss'))
        setHeader(attributes, Tag.Manufacturer, 'cat2xnat')
        setHeader(attributes, Tag.PatientName, name)
        setHeader(attributes, Tag.PatientID, name)
        setHeader(attributes, Tag.InstanceNumber, '1')
        setHeader(attributes, Tag.StudyDescription, 'Cat Scans')
        setHeader(attributes, Tag.SeriesDescription, 'CAT_SCAN')
        images.each { image ->
            final File jpegDicomFile = new File("${name}_${modality}_${image.index}_jpg.dcm")
            final File finalDicomFile = new File("${name}_${modality}_${image.index}.dcm")
            setHeader(attributes, Tag.SeriesInstanceUID, UIDUtils.createUID())
            setHeader(attributes, Tag.SOPInstanceUID, UIDUtils.createUID())
            setHeader(attributes, Tag.SeriesNumber, String.valueOf(image.index))
            final Jpg2Dcm jpg2Dcm = new Jpg2Dcm()
            jpg2Dcm.setMetadata(attributes)
            jpg2Dcm.convert(image.file, jpegDicomFile)
            image.file.delete()
            final Dcm2Dcm dcm2Dcm = new Dcm2Dcm()
            dcm2Dcm.setTransferSyntax(UID.ExplicitVRLittleEndian)
            dcm2Dcm.transcode(jpegDicomFile, finalDicomFile)
            println "${jpegDicomFile.name} -> ${finalDicomFile.name}"
            jpegDicomFile.delete()
        }
    }

    private void setHeader(Attributes headers, int header, String value) {
        headers.setString(header, elementDictionary.vrOf(header), value ?: '')
    }

}
