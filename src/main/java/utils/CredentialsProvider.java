package utils;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;

public class CredentialsProvider {
    private static AWSCredentialsProvider provider;

    private static void init() {
        BasicAWSCredentials credentials = new BasicAWSCredentials(
                Config.get(AppProperty.AWS_ID), Config.get(AppProperty.AWS_SECRET_KEY));
        provider = new AWSStaticCredentialsProvider(credentials);
    }

    public static AWSCredentialsProvider getProvider() {
        if (provider == null) {
            init();
        }

        return provider;
    }

    private CredentialsProvider() {

    }
}
