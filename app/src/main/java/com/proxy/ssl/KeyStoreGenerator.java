package com.proxy.ssl;

import android.content.Context;
import com.proxy.setting.Settings;
import com.proxy.utils.Networks;
import java.security.PrivateKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.RSAPrivateKeySpec;
import java.util.Enumeration;
import static java.util.Objects.requireNonNull;
import static org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers.pkcs_9_at_friendlyName;
import static org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers.pkcs_9_at_localKeyId;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;

import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.Signature;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.spec.RSAPrivateCrtKeySpec;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;
import java.util.Random;

import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Encoding;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1OutputStream;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.DERBMPString;
import org.bouncycastle.asn1.DERBitString;
import org.bouncycastle.asn1.DERNull;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.Certificate;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.Extensions;
import org.bouncycastle.asn1.x509.ExtensionsGenerator;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.asn1.x509.TBSCertificate;
import org.bouncycastle.asn1.x509.Time;
import org.bouncycastle.asn1.x509.V3TBSCertificateGenerator;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.crypto.params.RSAKeyParameters;
import org.bouncycastle.crypto.params.RSAPrivateCrtKeyParameters;
import org.bouncycastle.jcajce.provider.asymmetric.RSA;
import org.bouncycastle.jce.provider.X509CertificateObject;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

public class KeyStoreGenerator {

	private final X509Certificate rootCert;
	private final RSAKeyParameters privateKeyParameters;

	private final SecureRandom secureRandom;
	private final Random random;
	private final JcaX509ExtensionUtils jcaX509ExtensionUtils;

	static {
		Security.addProvider(new BouncyCastleProvider());
	}

	/**
	 * Constructs a new `KeyStoreGenerator`.  Loads the root certificate and
	 * private key from the specified PKCS12 keystore file (from assets).
	 *
	 * @param context              The Android `Context`.
	 * @param rootKeyStorePath     The path to the root keystore file in assets.
	 * @param rootKeyStorePassword The password for the root keystore.
	 * @throws Exception If an error occurs during keystore loading or processing.
	 */
	public KeyStoreGenerator(Context context, String rootKeyStorePath, char[] rootKeyStorePassword) throws Exception {
		KeyStore rootKeyStore = KeyStore.getInstance("PKCS12");
		try (InputStream input = context.getAssets().open("JPX.p12")) {
			rootKeyStore.load(input, rootKeyStorePassword);
		}

		Enumeration<String> aliases = rootKeyStore.aliases();
		String alias = aliases.nextElement();
		Key key = rootKeyStore.getKey(alias, Settings.rootKeyStorePassword);
		if (key instanceof RSAPrivateCrtKey) {
			// Extract CRT parameters from RSAPrivateCrtKey
			RSAPrivateCrtKey privateCrtKey = (RSAPrivateCrtKey) key;
			privateKeyParameters = getPrivateKeyParameters(privateCrtKey);
		} else if (key instanceof RSAPrivateKey) {
			// Handle non-CRT private keys (basic RSA keys)
			RSAPrivateKey rsaPrivateKey = (RSAPrivateKey) key;

			privateKeyParameters = getPrivateKeyParameters(rsaPrivateKey);
		} else {
			throw new IllegalStateException("Unsupported private key type: " + key.getClass());
		}

		rootCert = (X509Certificate) rootKeyStore.getCertificate(alias);
		requireNonNull(rootCert, "Specified certificate of the KeyStore not found!");

		rootCert.verify(rootCert.getPublicKey());

		secureRandom = new SecureRandom();
		random = new Random();
		jcaX509ExtensionUtils = new JcaX509ExtensionUtils();
	}

	public BigInteger getRootCertSN() {
		return rootCert.getSerialNumber();
	}

	/**
	 * return pub certificate byte data
	 *
	 * @param pem if false, return crt data; if true, return pem encoded data
	 */
	public byte[] exportRootCert(boolean pem) throws CertificateEncodingException {
		byte[] data = rootCert.getEncoded();
		if (!pem) {
			return data;
		}
		return ("-----BEGIN CERTIFICATE-----\n" + Base64.getMimeEncoder().encodeToString(data)
				+ "\n-----END CERTIFICATE-----\n").getBytes(StandardCharsets.US_ASCII);
	}

	/**
	* Extracts the private key parameters from an RSAPrivateCrtKey.
	*
	* @param privateCrtKey The RSAPrivateCrtKey.
	* @return The RSAPrivateCrtKeyParameters.
	*/
	private RSAPrivateCrtKeyParameters getPrivateKeyParameters(RSAPrivateCrtKey privateCrtKey) {
		return new RSAPrivateCrtKeyParameters(privateCrtKey.getModulus(), privateCrtKey.getPublicExponent(),
				privateCrtKey.getPrivateExponent(), privateCrtKey.getPrimeP(), privateCrtKey.getPrimeQ(),
				privateCrtKey.getPrimeExponentP(), privateCrtKey.getPrimeExponentQ(),
				privateCrtKey.getCrtCoefficient());
	}

	/**
	* Extracts the private key parameters from an RSAPrivateKey.
	*
	* @param privateKey The RSAPrivateKey.
	* @return The RSAKeyParameters.
	*/
	private RSAKeyParameters getPrivateKeyParameters(RSAPrivateKey privateKey) {
		return new RSAKeyParameters(true, privateKey.getModulus(), privateKey.getPrivateExponent());
	}

	/**
	* Generates a new keystore containing a certificate chain for the given host.
	*
	* @param host           The host name for the certificate.
	* @param validityDays   The validity period of the certificate in days.
	* @param keyStorePassword The password for the generated keystore.
	* @return The generated `KeyStore` object.
	* @throws Exception If an error occurs during certificate generation or
	*                   keystore creation.
	*/
	public KeyStore generateKeyStore(String host, int validityDays, char[] keyStorePassword) throws Exception {
		PrivateKeyAndCertChain keyAndCertChain = generateCertChain(host, validityDays);

		KeyStore store = KeyStore.getInstance("PKCS12");
		store.load(null, null);
		store.setKeyEntry(Settings.certAliasName, keyAndCertChain.privateKey(), keyStorePassword,
				keyAndCertChain.certificateChain());
		return store;
	}

	/**
	 * Generate cert for the domain signed by root certificate
	 * look at RFC 2818
	 *
	 * @param host add to san extension, can be generic
	 */
	public PrivateKeyAndCertChain generateCertChain(String host, int validityDays) throws Exception {

		// generate the key pair for the new certificate
		KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
		keyGen.initialize(2048, secureRandom);
		KeyPair keypair = keyGen.generateKeyPair();
		PrivateKey privateKey = keypair.getPrivate();
		PublicKey publicKey = keypair.getPublic();

		Calendar calendar = Calendar.getInstance();
		// in case client time behind server time
		calendar.add(Calendar.DAY_OF_YEAR, -1);
		Date startDate = calendar.getTime();
		calendar.setTime(new Date());
		calendar.add(Calendar.DAY_OF_YEAR, validityDays);
		Date expireDate = calendar.getTime();

		String appDName = "CN=ClearTheSky, OU=TianCao, O=TianCao, L=Beijing, ST=Beijing, C=CN";
		X500Name subject = new X500Name(appDName);
		ASN1ObjectIdentifier sigOID = PKCSObjectIdentifiers.sha256WithRSAEncryption;
		AlgorithmIdentifier sigAlgId = new AlgorithmIdentifier(sigOID, DERNull.INSTANCE);

		V3TBSCertificateGenerator generator = new V3TBSCertificateGenerator();
		generator.setSerialNumber(new ASN1Integer(random.nextLong() + System.currentTimeMillis()));
		generator.setIssuer(getSubject(rootCert));
		generator.setSubject(subject);
		generator.setSignature(sigAlgId);
		generator.setSubjectPublicKeyInfo(getPublicKeyInfo(publicKey));
		generator.setStartDate(new Time(startDate));
		generator.setEndDate(new Time(expireDate));

		// Set SubjectAlternativeName
		ExtensionsGenerator extensionsGenerator = new ExtensionsGenerator();
		extensionsGenerator.addExtension(Extension.subjectAlternativeName, false, () -> {
			ASN1EncodableVector nameVector = new ASN1EncodableVector();
			int hostType = Networks.getHostType(host);
			if (hostType == Networks.HOST_TYPE_IPV4 || hostType == Networks.HOST_TYPE_IPV6) {
				nameVector.add(new GeneralName(GeneralName.iPAddress, host));
			} else {
				nameVector.add(new GeneralName(GeneralName.dNSName, host));
			}
			return GeneralNames.getInstance(new DERSequence(nameVector)).toASN1Primitive();
		});
		Extensions x509Extensions = extensionsGenerator.generate();
		generator.setExtensions(x509Extensions);

		TBSCertificate tbsCertificateStructure = generator.generateTBSCertificate();
		byte[] data = toBinaryData(tbsCertificateStructure);
		byte[] signatureData = signData(sigOID, data, privateKeyParameters, secureRandom);

		ASN1EncodableVector asn1EncodableVector = new ASN1EncodableVector();
		asn1EncodableVector.add(tbsCertificateStructure);
		asn1EncodableVector.add(sigAlgId);
		asn1EncodableVector.add(new DERBitString(signatureData));

		DERSequence derSequence = new DERSequence(asn1EncodableVector);
		Certificate certificate = Certificate.getInstance(derSequence);
		X509CertificateObject clientCertificate = new X509CertificateObject(certificate);

		clientCertificate.verify(rootCert.getPublicKey());
		clientCertificate.setBagAttribute(pkcs_9_at_friendlyName, new DERBMPString("Certificate for CuteProxy App"));
		clientCertificate.setBagAttribute(pkcs_9_at_localKeyId,
				jcaX509ExtensionUtils.createSubjectKeyIdentifier(publicKey));

		return new PrivateKeyAndCertChain(privateKey, new X509Certificate[] { clientCertificate, rootCert });
	}

	private static byte[] signData(ASN1ObjectIdentifier sigOID, byte[] data, RSAKeyParameters privateKeyParameters,
			SecureRandom secureRandom) throws Exception {
		// Generate a PrivateKey from the RSAKeyParameters
		PrivateKey caPrivateKey;
		if (privateKeyParameters instanceof RSAPrivateCrtKeyParameters) {
			// If the key has CRT parameters, use them
			caPrivateKey = KeyFactory.getInstance("RSA")
					.generatePrivate(new RSAPrivateCrtKeySpec(privateKeyParameters.getModulus(),
							((RSAPrivateCrtKeyParameters) privateKeyParameters).getPublicExponent(),
							privateKeyParameters.getExponent(),
							((RSAPrivateCrtKeyParameters) privateKeyParameters).getP(),
							((RSAPrivateCrtKeyParameters) privateKeyParameters).getQ(),
							((RSAPrivateCrtKeyParameters) privateKeyParameters).getDP(),
							((RSAPrivateCrtKeyParameters) privateKeyParameters).getDQ(),
							((RSAPrivateCrtKeyParameters) privateKeyParameters).getQInv()));
		} else {
			// If the key does not have CRT parameters, use standard RSA private key spec
			caPrivateKey = KeyFactory.getInstance("RSA").generatePrivate(
					new RSAPrivateKeySpec(privateKeyParameters.getModulus(), privateKeyParameters.getExponent()));
		}

		// Sign the data using the private key
		Signature signature = Signature.getInstance(sigOID.getId());
		signature.initSign(caPrivateKey, secureRandom);
		signature.update(data);
		return signature.sign();
	}

	private static byte[] toBinaryData(TBSCertificate tbsCertificateStructure) throws IOException {
		byte[] data;
		try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
			ASN1OutputStream derOutputStream = ASN1OutputStream.create(bos, ASN1Encoding.DER);
			try {
				derOutputStream.writeObject(tbsCertificateStructure);
				data = bos.toByteArray();
			} finally {
				derOutputStream.close();
			}
		}
		return data;
	}

	private static X500Name getSubject(X509Certificate certificate) throws IOException, CertificateEncodingException {
		X509CertificateHolder certHolder = new X509CertificateHolder(certificate.getEncoded());
		return certHolder.getSubject();
	}

	private static SubjectPublicKeyInfo getPublicKeyInfo(PublicKey publicKey) throws IOException {
		try (ByteArrayInputStream bis = new ByteArrayInputStream(publicKey.getEncoded());
				ASN1InputStream asn1InputStream = new ASN1InputStream(bis)) {
			return SubjectPublicKeyInfo.getInstance(asn1InputStream.readObject());
		}
	}

	private static RSAPrivateCrtKeySpec getKeySpec(RSAPrivateCrtKeyParameters privateKeyParameters) {
		return new RSAPrivateCrtKeySpec(privateKeyParameters.getModulus(), privateKeyParameters.getPublicExponent(),
				privateKeyParameters.getExponent(), privateKeyParameters.getP(), privateKeyParameters.getQ(),
				privateKeyParameters.getDP(), privateKeyParameters.getDQ(), privateKeyParameters.getQInv());
	}

}