package com.hartle_klug.mhartle.sacon2019.control_plane.usecases;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.ExtendedKeyUsage;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.cert.CertIOException;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import com.google.common.base.Charsets;

import io.envoyproxy.controlplane.cache.SnapshotCache;
import io.envoyproxy.envoy.api.v2.auth.CertificateValidationContext;
import io.envoyproxy.envoy.api.v2.auth.Secret;
import io.envoyproxy.envoy.api.v2.auth.TlsCertificate;
import io.envoyproxy.envoy.api.v2.core.DataSource;

public abstract class AbstractUseCase implements UseCase {
	public abstract void apply(Map<String, List<String>> parameters, SnapshotCache<String> snapshotCache, long version) throws Exception;
	
	public abstract void unapply(SnapshotCache<String> snapshotCache, long version) throws Exception;
	
	protected static Secret createTlsCertificateSecret(final String name, final List<X509Certificate> certificateChain, final PrivateKey privateKey) throws IOException {
		final StringBuilder pemCertificateChainBuilder = new StringBuilder();
		for(final X509Certificate certificateChainLink : certificateChain) {
			pemCertificateChainBuilder.append(encodePEMObject(certificateChainLink) + "\n");
		}
		final String pemCertificateChain = pemCertificateChainBuilder.toString();

		final String pemPrivateKey = encodePEMObject(privateKey);

		final Secret result =
				Secret.newBuilder()
				.setName(name)
				.setTlsCertificate(
						TlsCertificate.newBuilder()
						.setCertificateChain(
								DataSource.newBuilder()
								.setInlineString(pemCertificateChain)
								.build())
						.setPrivateKey(
								DataSource.newBuilder()
								.setInlineString(pemPrivateKey)
								.build())
						.build())
				.build();
		
		return result;
	}
	
	protected static Secret createValidationContextSecret(final String name, final X509Certificate certificate) throws NoSuchAlgorithmException {
		final MessageDigest digest =
				MessageDigest.getInstance("SHA-256");
		
		final String spkiHash =
				Base64.getMimeEncoder()
				.encodeToString(
						digest.digest(
								certificate.getPublicKey()
								.getEncoded()));

		final Secret result =
				Secret.newBuilder()
				.setName(name)
				.setValidationContext(
						CertificateValidationContext.newBuilder()
						.addVerifyCertificateSpki(spkiHash)
						.build())
				.build();
		
		return result;
	}
	
	protected static X509Certificate generateCertificate(Provider provider, String issuer, PrivateKey issuerPrivateKey, String subject, PublicKey subjectPublicKey, Date notBefore, Date notAfter, BigInteger serialNumber, boolean certificateAuthority) throws OperatorCreationException, CertificateException, CertIOException {
		final ContentSigner contentSigner =
				new JcaContentSignerBuilder("SHA256WithRSA")
				.build(issuerPrivateKey);
		
		final JcaX509v3CertificateBuilder certificateBuilder =
				new JcaX509v3CertificateBuilder(
						new X500Name(issuer),
						serialNumber,
						notBefore,
						notAfter,
						new X500Name(subject),
						subjectPublicKey);
		
		// Indicate usage for digital signatures and key encipherment
		certificateBuilder.addExtension(
				Extension.keyUsage,
				true,
				new KeyUsage(KeyUsage.digitalSignature | KeyUsage.keyEncipherment));
				
		// Indicate usage for server and client authentication
		certificateBuilder.addExtension(
				Extension.extendedKeyUsage,
				false,
				new ExtendedKeyUsage(new KeyPurposeId[] {
						KeyPurposeId.id_kp_serverAuth,
						KeyPurposeId.id_kp_clientAuth }));
		
		if (certificateAuthority) {
			// Indicate subject acts as a CA
			certificateBuilder.addExtension(
					Extension.basicConstraints,
					true,
					new BasicConstraints(1));
		}

		final X509CertificateHolder certificateHolder =
				certificateBuilder.build(contentSigner);
		
		final JcaX509CertificateConverter certificateConverter = new JcaX509CertificateConverter();
		
		final X509Certificate result =
				certificateConverter
				.setProvider(provider)
				.getCertificate(certificateHolder);
		
		return result;
	}
	
	protected static String encodePEMObject(final Object object) throws IOException {
		final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		final Writer byteArrayWriter = new BufferedWriter(new OutputStreamWriter(byteArrayOutputStream));
		try(final JcaPEMWriter pemWriter = new JcaPEMWriter(byteArrayWriter)) {
			pemWriter.writeObject(object);
			pemWriter.flush();
		}
		final String result = byteArrayOutputStream.toString();
		return result;	
	}
	
	protected static X509Certificate decodePEMCertificate(final String pemCertificate) throws CertificateException, IOException {
		final CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
		try (final InputStream inputStream = new ByteArrayInputStream(pemCertificate.getBytes())) {
			final X509Certificate certificate = (X509Certificate) certificateFactory.generateCertificate(inputStream);
			return certificate;
		}
	}
	
	protected static X509Certificate readCertificate(final File file) throws CertificateException, IOException {
		try(final InputStream inputStream = new FileInputStream(file)) {
			final String pemCertificate = IOUtils.toString(inputStream, Charsets.US_ASCII);
			final X509Certificate certificate = decodePEMCertificate(pemCertificate);
			return certificate;
		}
	}
}
