#!/usr/bin/env groovy
import grepstar.dicom.*

@GrabResolver(name = 'dcm4che repo', root = 'http://www.dcm4che.org/maven2', m2Compatible = true)
@Grapes([
        @Grab(group = 'commons-io', module = 'commons-io', version = '2.5'),
        @Grab(group = 'org.dcm4che.tool', module = 'dcm4che-tool-jpg2dcm', version = '5.11.0'),
        @Grab(group = 'org.dcm4che.tool', module = 'dcm4che-tool-dcm2dcm', version = '5.11.0'),
        @Grab(group='joda-time', module='joda-time', version='2.9.9'),
        @Grab(group = 'com.jayway.restassured', module = 'rest-assured', version = '2.5.0'),
        @GrabExclude("org.codehaus.groovy:groovy-xml"),
        @GrabExclude("org.codehaus.groovy:groovy-json")
])

def cli = new CliBuilder()

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

final List<Image> images = Image.prepareImages(urls, local)
new ImageConverter().convert(images, catName, modality)