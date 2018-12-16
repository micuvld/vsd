package aws;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import utils.AppProperty;
import utils.Config;
import utils.CredentialsProvider;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class S3Wrapper {
    private AmazonS3 s3Client;

    public S3Wrapper() {
        s3Client = AmazonS3ClientBuilder.standard()
                .withCredentials(CredentialsProvider.getProvider())
                .withRegion(Config.get(AppProperty.AWS_REGION))
                .build();
    }

    public List<String> getKeysOfBucket(String bucket) {
        ListObjectsV2Request req = new ListObjectsV2Request()
                .withBucketName(bucket);
        return s3Client.listObjectsV2(req).getObjectSummaries().stream()
                .map(S3ObjectSummary::getKey)
                .collect(Collectors.toList());
    }

    public byte[] getObjectBytes(String bucket, String key) throws IOException {
        return IOUtils.toByteArray(s3Client.getObject(new GetObjectRequest(bucket, key)).getObjectContent());
    }

    public String getObjectContent(String bucket, String key) {
        return s3Client.getObjectAsString(bucket, key);
    }

    public void uploadBytes(String bucket, String key, byte[] bytes) {
        String fileName = key + "_bytes";
        File file = new File(fileName);
        try {
            FileUtils.writeByteArrayToFile(file, bytes);
            s3Client.putObject(new PutObjectRequest(bucket, key, file));
            file.delete();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void putObject(String bucket, String key, String content) {
        s3Client.putObject(bucket, key, content);
    }

    public void clearBucket(String bucket) {
        ListObjectsV2Request req = new ListObjectsV2Request().withBucketName(bucket);
        ListObjectsV2Result result;

        do {
            result = s3Client.listObjectsV2(req);
            result.getObjectSummaries().forEach(object -> s3Client.deleteObject(bucket, object.getKey()));

            String token = result.getNextContinuationToken();
            req.setContinuationToken(token);
        } while (result.isTruncated());
    }
}
