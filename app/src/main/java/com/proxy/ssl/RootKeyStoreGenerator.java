package com.proxy.ssl;

import com.proxy.setting.Settings;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.*;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.bc.BcX509ExtensionUtils;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.Date;

/**
 * `RootKeyStoreGenerator` is a singleton class responsible for generating a
 * self-signed root certificate and storing it in a PKCS12 keystore.  This
 * keystore can then be used to generate certificates for other entities.
 */
public class RootKeyStoreGenerator {

	private static final RootKeyStoreGenerator instance = new RootKeyStoreGenerator();
	private static final Provider PROVIDER = new BouncyCastleProvider();

	private RootKeyStoreGenerator() {
	}

	/**
	 * Gets the singleton instance of `RootKeyStoreGenerator`.
	 *
	 * @return The singleton instance.
	 */
	public static RootKeyStoreGenerator getInstance() {
		return instance;
	}

	/**
	 * Generates a self-signed root certificate and stores it in a PKCS12
	 * keystore.
	 *
	 * @param password      The password for the generated keystore.
	 * @param validityDays  The validity period of the certificate in days.
	 * @return The generated keystore as a byte array.
	 * @throws Exception If an error occurs during certificate generation or
	 *                   keystore creation.
	 */
	public byte[] generate(char[] password, int validityDays) throws Exception {
		SecureRandom secureRandom = new SecureRandom();
		KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
		keyGen.initialize(2048, secureRandom);
		KeyPair keypair = keyGen.generateKeyPair();
		PrivateKey privateKey = keypair.getPrivate();
		PublicKey publicKey = keypair.getPublic();

		KeyStore keyStore = KeyStore.getInstance("PKCS12");
		keyStore.load(null, null);

		Security.addProvider(new BouncyCastleProvider());

		String appDName = "CN=JPX , OU=JPX , O=MR.ETHICAL YT, L=SOUTH INDIA, ST=SOUTH INDIA, C=IN";
		X500Name issuerName = new X500Name(appDName);
		X500Name subjectName = new X500Name(appDName);
		Calendar calendar = Calendar.getInstance();
		// in case client time behind server time
		calendar.add(Calendar.DAY_OF_YEAR, -100);
		Date startDate = calendar.getTime();
		calendar.add(Calendar.DAY_OF_YEAR, validityDays);
		Date endDate = calendar.getTime();

		byte[] encoded = publicKey.getEncoded();
		SubjectPublicKeyInfo subjectPublicKeyInfo = SubjectPublicKeyInfo.getInstance(ASN1Sequence.getInstance(encoded));
		X509v3CertificateBuilder builder = new X509v3CertificateBuilder(issuerName,
				BigInteger.valueOf(secureRandom.nextLong() + System.currentTimeMillis()), startDate, endDate,
				subjectName, subjectPublicKeyInfo);

		builder.addExtension(Extension.subjectKeyIdentifier, false, createSubjectKeyIdentifier(publicKey));
		builder.addExtension(Extension.basicConstraints, true, new BasicConstraints(true));
		KeyUsage usage = new KeyUsage(KeyUsage.keyCertSign | KeyUsage.digitalSignature | KeyUsage.keyEncipherment
				| KeyUsage.dataEncipherment | KeyUsage.cRLSign);
		builder.addExtension(Extension.keyUsage, false, usage);

		ASN1EncodableVector purposes = new ASN1EncodableVector();
		purposes.add(KeyPurposeId.id_kp_serverAuth);
		purposes.add(KeyPurposeId.id_kp_clientAuth);
		purposes.add(KeyPurposeId.anyExtendedKeyUsage);
		builder.addExtension(Extension.extendedKeyUsage, false, new DERSequence(purposes));

		X509Certificate cert = signCertificate(builder, privateKey);
		cert.checkValidity(new Date());
		cert.verify(publicKey);

		X509Certificate[] chain = new X509Certificate[] { cert };

		keyStore.setEntry("JPX", new KeyStore.PrivateKeyEntry(privateKey, chain),
				new KeyStore.PasswordProtection(Settings.rootKeyStorePassword));
		try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
			keyStore.store(bos, password);
			return bos.toByteArray();
		}
	}

	/**
	 * Creates a Subject Key Identifier for the given public key.  This is an
	 * X.509 extension that uniquely identifies the subject's public key.
	 *
	 * @param key The public key.
	 * @return The SubjectKeyIdentifier.
	 * @throws IOException If an I/O error occurs.
	 */
	private static SubjectKeyIdentifier createSubjectKeyIdentifier(Key key) throws IOException {
		try (ASN1InputStream is = new ASN1InputStream(new ByteArrayInputStream(key.getEncoded()))) {
			ASN1Sequence seq = (ASN1Sequence) is.readObject();
			SubjectPublicKeyInfo info = SubjectPublicKeyInfo.getInstance(seq);
			return new BcX509ExtensionUtils().createSubjectKeyIdentifier(info);
		}
	}

	private static X509Certificate signCertificates(X509v3CertificateBuilder certificateBuilder,
			PrivateKey signedWithPrivateKey) throws OperatorCreationException, CertificateException {
		ContentSigner signer = new JcaContentSignerBuilder("SHA256WithRSAEncryption")
				.setProvider(BouncyCastleProvider.PROVIDER_NAME).build(signedWithPrivateKey);
		return new JcaX509CertificateConverter().setProvider(BouncyCastleProvider.PROVIDER_NAME)
				.getCertificate(certificateBuilder.build(signer));
	}

	private static X509Certificate signCertificate(X509v3CertificateBuilder x509v3CertificateBuilder,
			PrivateKey privateKey) throws OperatorCreationException, CertificateException {
		JcaContentSignerBuilder jcaContentSignerBuilder = new JcaContentSignerBuilder("SHA256WithRSAEncryption");
		Provider provider = PROVIDER;
		return new JcaX509CertificateConverter().setProvider(provider).getCertificate(
				x509v3CertificateBuilder.build(jcaContentSignerBuilder.setProvider(provider).build(privateKey)));
	}
}