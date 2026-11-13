import com.android.apksig.ApkVerifier;

import java.io.File;
import java.security.MessageDigest;
import java.security.cert.X509Certificate;
import java.util.List;

/** Prints Google's own APK signature verification result for a file. */
public class VerifyApk {
    public static void main(String[] args) throws Exception {
        for (String path : args) {
            System.out.println("=== " + path + " ===");
            ApkVerifier.Builder b = new ApkVerifier.Builder(new File(path));
            if (System.getProperty("mincheck") != null) {
                b.setMinCheckedPlatformVersion(Integer.parseInt(System.getProperty("mincheck")));
            }
            ApkVerifier verifier = b.build();
            ApkVerifier.Result r = verifier.verify();
            System.out.println("  overall verified   : " + r.isVerified());
            System.out.println("  v1 (JAR) verified  : " + r.isVerifiedUsingV1Scheme());
            System.out.println("  v2 verified        : " + r.isVerifiedUsingV2Scheme());
            System.out.println("  v3 verified        : " + r.isVerifiedUsingV3Scheme());
            System.out.println("  v3.1 verified      : " + r.isVerifiedUsingV31Scheme());
            System.out.println("  v4 verified        : " + r.isVerifiedUsingV4Scheme());
            List<X509Certificate> certs = r.getSignerCertificates();
            System.out.println("  signers            : " + (certs == null ? 0 : certs.size()));
            if (certs != null) {
                for (X509Certificate c : certs) {
                    byte[] d = MessageDigest.getInstance("SHA-256").digest(c.getEncoded());
                    StringBuilder sb = new StringBuilder();
                    for (byte x : d) sb.append(String.format("%02x", x));
                    System.out.println("    subject : " + c.getSubjectX500Principal());
                    System.out.println("    issuer  : " + c.getIssuerX500Principal());
                    System.out.println("    serial  : " + c.getSerialNumber().toString(16));
                    System.out.println("    valid   : " + c.getNotBefore() + " -> " + c.getNotAfter());
                    System.out.println("    sha256  : " + sb);
                }
            }
            List<ApkVerifier.IssueWithParams> errs = r.getErrors();
            if (errs != null) {
                for (ApkVerifier.IssueWithParams e : errs) {
                    System.out.println("  ERROR: " + e.getIssue());
                }
            }
            List<ApkVerifier.IssueWithParams> warns = r.getWarnings();
            if (warns != null) {
                for (ApkVerifier.IssueWithParams w : warns) {
                    System.out.println("  WARN : " + w.getIssue());
                }
            }
        }
    }
}
