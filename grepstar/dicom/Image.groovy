package grepstar.dicom

import com.jayway.restassured.RestAssured
import org.apache.commons.io.FileUtils

class Image {

    String source
    File file
    int index

    Image file(File file) {
        setFile(file)
        this
    }

    Image source(String source) {
        setSource(source)
        this
    }

    Image index(int index) {
        setIndex(index)
        this
    }

    static List<Image> prepareImages(List<String> urls, List<String> localImages) {
        final List<Image> images = []
        urls.eachWithIndex { url, index ->
            final File image = new File("downloaded_image_${index + 1}.jpg")
            FileUtils.copyInputStreamToFile(RestAssured.given().get(url).then().assertThat().statusCode(200).and().extract().response().asInputStream(), image)
            println "Downloaded file from ${url}..."
            images << new Image().source(url).file(image).index(index + 1)
        }
        localImages.eachWithIndex { file, index ->
            images << new Image().source('local image').file(new File(file)).index(urls.size() + index + 1)
        }
        images
    }

}
