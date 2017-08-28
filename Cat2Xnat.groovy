#!/usr/bin/env groovy
import com.jayway.restassured.RestAssured
import org.apache.commons.io.FileUtils

@Grapes([
        @Grab(group = "commons-io", module = "commons-io", version = "2.5"),
        @Grab(group = "com.jayway.restassured", module = "rest-assured", version = "2.5.0"),
        @GrabExclude("org.codehaus.groovy:groovy-xml"),
        @GrabExclude("org.codehaus.groovy:groovy-json")
])

def cli = new CliBuilder(
        usage: 'groovy Cat2Xnat.groovy -n name -d targetData',
        header: '\nParameters:\n',
        footer: 'The data parameter (-d) specifies a text file that contains newline separated URLs to pull images from.')

cli.with {
    h(longOpt: 'help', 'Usage Information', required: false)
    d(longOpt: 'data', 'Text file containing list of image URLs (separated by newlines).', args: 1, required: false)
    l(longOpt: 'local', 'Text file containing list of image files in directory to process (separated by newlines).', args: 1, required: false)
    n(longOpt: 'name', 'Name of the cat.', args: 1, required: true)
    m(longOpt: 'modality', 'DICOM modality to insert into (0008,0060). Defaults to MR.', args: 1, required: false)
}

if ('-h' in args || '--help' in args || args.size() == 0) {
    cli.usage()
    return
}

def params = cli.parse(args)
final String catName = params.n as String
final List<String> urls = (params.d) ? new File(params.d as String).readLines() : []
final List<String> local = (params.l) ? new File(params.l as String).readLines() : []
final String modality = params.m ?: 'MR'

String firstFile = null

final Map<String, String> files = [:]
urls.eachWithIndex { url, index ->
    final File image = new File("downloaded_image_${index}")
    FileUtils.copyInputStreamToFile(RestAssured.given().get(url).then().assertThat().statusCode(200).and().extract().response().asInputStream(), image)
    println "Downloaded file from ${url}..."
    files.put(image.name, url)
}
local.each { file ->
    files.put(file, null)
}

files.eachWithIndex { fileName, source, index ->
    final String sopClassUID
    if (modality == 'MR') {
        sopClassUID = '1.2.840.10008.5.1.4.1.1.4'
    } else if (modality == 'PT') {
        sopClassUID = '1.2.840.10008.5.1.4.1.​1.​128'
    } else if (modality == 'CT') {
        sopClassUID = '1.2.840.10008.5.1.4.1.1.2'
    } else {
        sopClassUID = '1.2.840.10008.5.1.4.1.1.7'
    }

    final String patientDataSource = (firstFile == null) ? "-k 0010,0010=${catName} -k 0010,0020=${catName}_${modality}" : "-stf ${firstFile}"
    final String dicomFileName = "${catName}_${modality}_${index}.dcm"
    final String sourceCommand = (source == null) ? '' : "-k 0008,4000=${source}"

    final StringBuffer stdOut = new StringBuffer()
    final StringBuffer stdErr = new StringBuffer()
    final Process process = "img2dcm -k 0008,0016=${sopClassUID} -k 0008,0060=${modality} -k 0020,0011=${index} -k 0008,103E=Cat_Scan ${sourceCommand} ${patientDataSource} ${fileName} ${dicomFileName}".execute()
    process.consumeProcessOutput(stdOut, stdErr)
    process.waitForOrKill(10000)
    if (stdOut) println stdOut
    if (stdErr) println stdErr
    if (firstFile == null) firstFile = dicomFileName
}