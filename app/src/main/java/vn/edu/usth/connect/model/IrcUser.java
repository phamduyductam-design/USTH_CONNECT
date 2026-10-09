package vn.edu.usth.connect.model;

public final class IrcUser {
  public final String nickname, prefixes;

  public IrcUser(String nickname, String prefixes) {
    this.nickname = nickname;
    this.prefixes = prefixes;
  }
}
