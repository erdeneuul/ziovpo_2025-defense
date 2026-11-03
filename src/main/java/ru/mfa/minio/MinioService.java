package ru.mfa.minio;

import io.minio.*;
import io.minio.http.Method;
import io.minio.GetPresignedObjectUrlArgs;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.util.concurrent.TimeUnit;

@Service
public class MinioService {

    private final MinioClient minioClient;
    private final MinioProperties properties;

    public MinioService(MinioClient minioClient, MinioProperties properties) {
        this.minioClient = minioClient;
        this.properties = properties;
    }

    public String uploadFile(String objectName, byte[] data, String contentType) throws Exception {
        String bucket = properties.getBucket();
        boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
        if (!exists) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
        }
        minioClient.putObject(PutObjectArgs.builder()
                .bucket(bucket)
                .object(objectName)
                .stream(new ByteArrayInputStream(data), data.length, -1)
                .contentType(contentType == null ? "application/octet-stream" : contentType)
                .build());
        return objectName;
    }

    public String getPresignedUrl(String objectName) throws Exception {
        return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                .method(Method.GET)
                .bucket(properties.getBucket())
                .object(objectName)
                .expiry(1, TimeUnit.HOURS)
                .build());
    }
}
