package buerostack.config;
import com.nimbusds.jose.*; import com.nimbusds.jose.crypto.*; import com.nimbusds.jose.jwk.*; import com.nimbusds.jwt.*;
import java.io.FileInputStream; import java.security.*; import java.security.cert.Certificate; import java.time.Instant; import java.util.*;
public class JwtSignerService {
  private final RSAKey rsaJwk; private final JWSHeader header;
  public JwtSignerService(String path,String type,String pass,String alias){
    try{
      KeyStore ks=KeyStore.getInstance(type); try(FileInputStream in=new FileInputStream(path.replace("file:",""))){ ks.load(in, pass.toCharArray()); }
      PrivateKey pk=(PrivateKey)ks.getKey(alias, pass.toCharArray()); Certificate cert=ks.getCertificate(alias);
      rsaJwk=new RSAKey.Builder((java.security.interfaces.RSAPublicKey)cert.getPublicKey()).privateKey(pk).keyID(alias).build();
      header=new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(rsaJwk.getKeyID()).build();
    }catch(Exception e){ throw new RuntimeException(e); }
  }
  public String sign(Map<String,Object> identity,String iss,String aud,long ttl) throws JOSEException {
    Instant now=Instant.now(); JWTClaimsSet.Builder cb=new JWTClaimsSet.Builder().issuer(iss).issueTime(Date.from(now)).expirationTime(Date.from(now.plusSeconds(ttl))).jwtID(UUID.randomUUID().toString()).audience(aud);
    identity.forEach(cb::claim); SignedJWT jwt=new SignedJWT(header, cb.build()); jwt.sign(new RSASSASigner(rsaJwk)); return jwt.serialize();
  }
  public String sign(Map<String,Object> identity,String iss,List<String> audiences,long ttl) throws JOSEException {
    Instant now=Instant.now(); JWTClaimsSet.Builder cb=new JWTClaimsSet.Builder().issuer(iss).issueTime(Date.from(now)).expirationTime(Date.from(now.plusSeconds(ttl))).jwtID(UUID.randomUUID().toString());
    cb.audience(audiences); identity.forEach(cb::claim); SignedJWT jwt=new SignedJWT(header, cb.build()); jwt.sign(new RSASSASigner(rsaJwk)); return jwt.serialize();
  }
  public boolean verify(String token){ try{ SignedJWT jwt=SignedJWT.parse(token); return jwt.verify(new RSASSAVerifier(rsaJwk.toPublicJWK().toRSAKey())); }catch(Exception e){ return false; } }
  public String publicJwkSet(){ return new JWKSet(rsaJwk.toPublicJWK()).toJSONObject().toString(); }
}