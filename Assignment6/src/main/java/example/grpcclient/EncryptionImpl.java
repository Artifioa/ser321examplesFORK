package example.grpcclient;

import io.grpc.stub.StreamObserver;
import service.*;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

class EncryptionImpl extends EncryptionGrpc.EncryptionImplBase {
    private static final String ALGORITHM = "AES";
    private static final byte[] KEY = "MySuperSecretKey".getBytes();

    @Override
    public void encrypt(EncryptRequest req, StreamObserver<EncryptResponse> responseObserver) {
        EncryptResponse.Builder response = EncryptResponse.newBuilder();
        System.out.println("Received from client: " + req.getInput());
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            SecretKeySpec secretKey = new SecretKeySpec(KEY, ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            String encryptedString = Base64.getEncoder().encodeToString(cipher.doFinal(req.getInput().getBytes()));
            response.setIsSuccess(true);
            response.setSolution(encryptedString);
        } catch (Exception e) {
            response.setIsSuccess(false);
            response.setError(e.getMessage());
        }
        responseObserver.onNext(response.build());
        responseObserver.onCompleted();
    }

    @Override
    public void decrypt(DecryptRequest req, StreamObserver<DecryptResponse> responseObserver) {
        DecryptResponse.Builder response = DecryptResponse.newBuilder();
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            SecretKeySpec secretKey = new SecretKeySpec(KEY, ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            String decryptedString = new String(cipher.doFinal(Base64.getDecoder().decode(req.getInput())));
            response.setIsSuccess(true);
            response.setSolution(decryptedString);
        } catch (Exception e) {
            response.setIsSuccess(false);
            response.setError(e.getMessage());
        }
        responseObserver.onNext(response.build());
        responseObserver.onCompleted();
    }
}