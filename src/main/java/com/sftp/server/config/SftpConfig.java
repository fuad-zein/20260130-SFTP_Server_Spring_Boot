package com.sftp.server.config;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;

import org.apache.sshd.common.file.virtualfs.VirtualFileSystemFactory;
import org.apache.sshd.common.session.SessionContext;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.auth.password.PasswordAuthenticator;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.sftp.server.SftpSubsystemFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

@Configuration
public class SftpConfig {

  @Value("${sftp.server.port:2222}")
  private int sftpPort;

  private final Map<String, String> userPasswords = Map.of("user1", "pass123", "user2", "pass456");

  private final Map<String, Path> userFolders = Map.of(
      "user1",
      Paths.get("/home/fuzi/Documents/Experiments/StoreFolders/user1").toAbsolutePath(),
      "user2",
      Paths.get("/home/fuzi/Documents/Experiments/StoreFolders/user2").toAbsolutePath());

  @PostConstruct
  public void startSftpServer() throws IOException {
    SshServer sshd = SshServer.setUpDefaultServer();
    sshd.setPort(sftpPort); // * port sftp

    sshd.setKeyPairProvider(new SimpleGeneratorHostKeyProvider(Paths.get("hostkey.ser")));
    sshd.setPasswordAuthenticator(passwordAuthenticator());

    // ? setup default folder for user
    sshd.setFileSystemFactory(fileSystemFactory());

    // ? activate subsystem sftp
    sshd.setSubsystemFactories(Collections.singletonList(new SftpSubsystemFactory()));
    sshd.start();
    System.out.println("✅ SFTP Server started on port " + sftpPort);
  }

  private PasswordAuthenticator passwordAuthenticator() {
    return (username, password, session) -> {
      String expectedPassword = userPasswords.get(username);

      if (expectedPassword == null) {
        System.out.println("❌ Unknown user: " + username);
        return false;
      }

      boolean valid = expectedPassword.equals(password);
      System.out.println(valid ? "✅ Auth success: " + username : "❌ Auth failed: " + username);
      return valid;
    };
  }

  private VirtualFileSystemFactory fileSystemFactory() {
    return new VirtualFileSystemFactory() {
      @Override
      public FileSystem createFileSystem(SessionContext session) throws IOException {
        String username = session.getUsername();
        Path homeDir = userFolders.get(username);

        if (homeDir == null) {
          throw new IOException("Home directory not configured for user: " + username);
        }

        setUserHomeDir(username, homeDir);
        return super.createFileSystem(session);
      }
    };
  }

}
