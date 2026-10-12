package vn.edu.usth.connect.repository;

import java.util.*;
import vn.edu.usth.connect.irc.*;
import vn.edu.usth.connect.model.*;

/**
 * Protocol-driven state. All access in Android is confined to the main thread by SessionRepository.
 */
public final class SessionState {
  public final Map<String, Channel> channels = new LinkedHashMap<>();
  public final Map<String, Conversation> conversations = new LinkedHashMap<>();
  public final Map<String, ServerChannel> availableChannels = new LinkedHashMap<>();
  public final Set<String> onlineUsers = new HashSet<>();
  public final Map<String, String> awayUsers = new HashMap<>();
  public boolean listing, listLoaded, away;
  public String awayMessage = "", requestedAway = "", errorDetail = "";
  public String prefixModes = "qaohv", prefixSymbols = "~&@%+", channelModes = "beI,k,l,imnpst";
  public String nickname = "", activeTarget = "", errorCode = "", errorTarget = "";
  public ConnectionState connection = ConnectionState.DISCONNECTED;

  public Channel channel(String target) {
    return channels.computeIfAbsent(IrcParser.fold(target), k -> new Channel(target));
  }

  public Conversation conversation(String target) {
    return IrcParser.isChannel(target)
        ? channel(target)
        : conversations.computeIfAbsent(IrcParser.fold(target), k -> new Conversation(target));
  }

  public void connectionLost() {
    listing = false;
    listLoaded = false;
    away = false;
    awayMessage = "";
    awayUsers.clear(); onlineUsers.clear();
    for (Channel c : channels.values()) {
      c.joined = false;
      c.joining = false;
      c.members.clear();
      c.modes.clear();
      c.namesBatch.clear();
      c.namesRemoved.clear();
      c.namesInProgress = false;
    }
    for (Conversation c : all())
      for (ChatMessage m : c.messages)
        if (m.status == ChatMessage.Status.QUEUED) m.status = ChatMessage.Status.UNCERTAIN;
  }

  public List<Conversation> all() {
    List<Conversation> all = new ArrayList<>(channels.values());
    all.addAll(conversations.values());
    return all;
  }

  public void markRead(String target) {
    Conversation c = conversation(target);
    c.unread = 0;
    c.firstUnreadId = 0;
  }

  public ChatMessage accept(IrcMessage m) {
    String from = m.nick(), folded = IrcParser.fold(from);
    boolean self = folded.equals(IrcParser.fold(nickname));
    Channel c;
    switch (m.command) {
      case "001":
        nickname = m.param(0); onlineUsers.add(IrcParser.fold(nickname));
        break;
      case "005":
        for (String token : m.parameters) {
          if (token.startsWith("PREFIX=(")) {
            int end = token.indexOf(')');
            if (end > 8 && token.length() - end - 1 == end - 8) {
              prefixModes = token.substring(8, end); prefixSymbols = token.substring(end + 1);
            }
          } else if (token.startsWith("CHANMODES=")) channelModes = token.substring(10);
        }
        break;
      case "321": availableChannels.clear(); listing = true; break;
      case "322":
        try {
          if (availableChannels.size() < 5000)
            availableChannels.put(IrcParser.fold(m.param(1)),
                new ServerChannel(m.param(1), Integer.parseInt(m.param(2)), m.last()));
        } catch (NumberFormatException ignored) { }
        break;
      case "323": listing = false; listLoaded = true; break;
      case "MODE":
        if (IrcParser.isChannel(m.param(0)))
          applyModes(channel(m.param(0)), m.param(1), m.parameters.subList(Math.min(2, m.parameters.size()), m.parameters.size()));
        break;
      case "324":
        c = channel(m.param(1)); c.modes.clear();
        applyModes(c, m.param(2), m.parameters.subList(Math.min(3, m.parameters.size()), m.parameters.size()));
        break;
      case "301": onlineUsers.add(IrcParser.fold(m.param(1))); awayUsers.put(IrcParser.fold(m.param(1)), m.last()); break;
      case "305": away = false; awayMessage = ""; awayUsers.remove(IrcParser.fold(nickname)); break;
      case "306": away = true; awayMessage = requestedAway; awayUsers.put(IrcParser.fold(nickname), awayMessage); break;
      case "AWAY":
        if (m.parameters.isEmpty()) awayUsers.remove(folded); else awayUsers.put(folded, m.last());
        break;
      case "352":
        String whoNick = m.param(5), flags = m.param(6);
        onlineUsers.add(IrcParser.fold(whoNick));
        if (flags.startsWith("G")) awayUsers.putIfAbsent(IrcParser.fold(whoNick), "");
        else if (flags.startsWith("H")) awayUsers.remove(IrcParser.fold(whoNick));
        Channel whoChannel = channels.get(IrcParser.fold(m.param(1)));
        if (whoChannel != null && whoChannel.joined && whoChannel.members.containsKey(IrcParser.fold(whoNick))) {
          String prefixes = "";
          for (char symbol : prefixSymbols.toCharArray()) if (flags.indexOf(symbol) >= 0) prefixes += symbol;
          memberAdd(whoChannel, new IrcUser(whoNick, prefixes));
        }
        break;
      case "JOIN":
        onlineUsers.add(folded);
        c = channel(m.last());
        if (self) {
          c.joined = true;
          c.wanted = true;
          c.joining = false;
          c.namesBatch.clear();
          c.namesRemoved.clear();
          c.namesInProgress = true;
        }
        memberAdd(c, new IrcUser(from, ""));
        event(c, from, "", "JOIN");
        break;
      case "PART":
        c = channel(m.param(0));
        memberRemove(c, folded);
        if (self) {
          c.joined = false;
          c.wanted = false;
          c.joining = false;
          c.members.clear();
          c.namesBatch.clear();
          c.namesRemoved.clear();
          c.namesInProgress = false;
        }
        event(c, from, "", "PART");
        break;
      case "KICK":
        c = channel(m.param(0));
        memberRemove(c, IrcParser.fold(m.param(1)));
        if (IrcParser.fold(m.param(1)).equals(IrcParser.fold(nickname))) {
          c.joined = false;
          c.wanted = false;
          c.members.clear();
          c.namesBatch.clear();
          c.namesRemoved.clear();
          c.namesInProgress = false;
        }
        event(c, m.param(1), "", "KICK");
        break;
      case "QUIT":
        awayUsers.remove(folded); onlineUsers.remove(folded);
        for (Channel ch : channels.values())
          if (ch.members.containsKey(folded) || ch.namesBatch.containsKey(folded)) {
            memberRemove(ch, folded);
            event(ch, from, "", "QUIT");
          }
        break;
      case "NICK":
        String newNick = m.last(), key = IrcParser.fold(newNick);
        if (onlineUsers.remove(folded)) onlineUsers.add(key);
        if (awayUsers.containsKey(folded)) awayUsers.put(key, awayUsers.remove(folded));
        for (Channel ch : channels.values()) {
          IrcUser user = ch.members.get(folded);
          if (user != null || ch.namesBatch.containsKey(folded)) {
            memberRemove(ch, folded);
            memberAdd(ch, new IrcUser(newNick, user == null ? "" : user.prefixes));
            event(ch, from, newNick, "NICK");
          }
        }
        if (self) nickname = newNick;
        Conversation pm = conversations.remove(folded);
        if (pm != null) {
          pm.target = newNick;
          Conversation existing = conversations.remove(key);
          if (existing != null && existing != pm) {
            pm.messages.addAll(existing.messages);
            pm.messages.sort(Comparator.comparingLong(a -> a.timestamp));
            while (pm.messages.size() > 500) pm.messages.remove(0);
            pm.unread += existing.unread;
          }
          conversations.put(key, pm);
          if (IrcParser.fold(activeTarget).equals(folded)) activeTarget = newNick;
        }
        break;
      case "353":
        c = channel(m.param(2));
        if (!c.joined) break;
        if (!c.namesInProgress) {
          c.namesInProgress = true;
          c.namesBatch.clear();
          c.namesRemoved.clear();
        }
        for (String value : m.last().split(" +")) {
          int i = 0;
          while (i < value.length() && prefixSymbols.indexOf(value.charAt(i)) >= 0) i++;
          String nick = value.substring(i).split("!", 2)[0];
          if (!nick.isEmpty() && !c.namesRemoved.contains(IrcParser.fold(nick))) {
            c.namesBatch.put(IrcParser.fold(nick), new IrcUser(nick, value.substring(0, i)));
            onlineUsers.add(IrcParser.fold(nick));
          }
        }
        break;
      case "366":
        c = channel(m.param(1));
        if (!c.joined) break;
        if (c.namesInProgress) {
          c.members.clear();
          c.members.putAll(c.namesBatch);
          c.namesBatch.clear();
          c.namesRemoved.clear();
          c.namesInProgress = false;
        }
        break;
      case "332":
        channel(m.param(1)).topic = m.last();
        break;
      case "331":
        channel(m.param(1)).topic = "";
        break;
      case "TOPIC":
        channel(m.param(0)).topic = m.last();
        break;
      case "PRIVMSG":
        String target = IrcParser.isChannel(m.param(0)) ? m.param(0) : from;
        Conversation chat = conversation(target);
        String text = m.last();
        if (text.startsWith("\u0001")) {
          if (text.startsWith("\u0001ACTION ") && text.endsWith("\u0001"))
            text = "• " + text.substring(8, text.length() - 1);
          else return null; // CTCP queries are not displayed as chat or automatically answered.
        }
        if (self) { // Defensive echo deduplication; we never request echo-message.
          for (int i = chat.messages.size() - 1; i >= Math.max(0, chat.messages.size() - 10); i--) {
            ChatMessage prior = chat.messages.get(i);
            if (prior.outgoing
                && prior.text.equals(text)
                && System.currentTimeMillis() - prior.timestamp < 10000) return null;
          }
        }
        ChatMessage message = new ChatMessage(from, text, self, ChatMessage.Status.RECEIVED);
        if (!self && !Mentions.ranges(text, nickname).isEmpty()) message.mentionNick = nickname;
        chat.add(message);
        if (!self && !IrcParser.fold(target).equals(IrcParser.fold(activeTarget))) {
          chat.unread++;
          if (chat.firstUnreadId == 0) chat.firstUnreadId = message.id;
        }
        return message;
      default:
        if (m.command.matches("[45][0-9]{2}")) {
          errorCode = m.command;
          errorDetail = m.last();
          if (m.param(1).equalsIgnoreCase("LIST")) listing = false;
          errorTarget = m.param(1);
          Channel failed = channels.get(IrcParser.fold(errorTarget));
          if (failed != null && !failed.joined) {
            failed.joining = false;
            failed.wanted = false;
          }
          if (Arrays.asList("401", "404", "407", "411", "412", "421", "442", "482")
              .contains(m.command)) {
            Conversation failedChat =
                IrcParser.isChannel(errorTarget)
                    ? failed
                    : conversations.get(IrcParser.fold(errorTarget));
            if (failedChat != null) {
              for (int i = failedChat.messages.size() - 1; i >= 0; i--) {
                ChatMessage sent = failedChat.messages.get(i);
                if (sent.outgoing && System.currentTimeMillis() - sent.timestamp < 30000) {
                  sent.status = ChatMessage.Status.FAILED;
                  break;
                }
              }
            }
          }
        }
    }
    return null;
  }

  public String role(String target, String nick) {
    Channel c = channels.get(IrcParser.fold(target));
    IrcUser user = c == null ? null : c.members.get(IrcParser.fold(nick));
    if (user != null) {
      for (int i = 0; i < prefixSymbols.length(); i++)
        if (user.prefixes.indexOf(prefixSymbols.charAt(i)) >= 0) return String.valueOf(prefixModes.charAt(i));
    }
    return "u";
  }

  public boolean canManage(String target) { return "qaoh".contains(role(target, nickname)); }

  private void applyModes(Channel c, String flags, List<String> args) {
    boolean adding = true; int at = 0;
    String[] groups = channelModes.split(",", -1);
    for (char mode : flags.toCharArray()) {
      if (mode == '+') { adding = true; continue; }
      if (mode == '-') { adding = false; continue; }
      int rank = prefixModes.indexOf(mode);
      boolean consumes = rank >= 0 || (groups.length > 0 && groups[0].indexOf(mode) >= 0)
          || (groups.length > 1 && groups[1].indexOf(mode) >= 0)
          || (adding && groups.length > 2 && groups[2].indexOf(mode) >= 0);
      String arg = consumes && at < args.size() ? args.get(at++) : "";
      if (rank >= 0) {
        if (arg.isEmpty()) continue;
        String key = IrcParser.fold(arg);
        updatePrefix(c.members, key, prefixSymbols.charAt(rank), adding);
        updatePrefix(c.namesBatch, key, prefixSymbols.charAt(rank), adding);
      } else if (adding) c.modes.add(mode); else c.modes.remove(mode);
    }
  }

  private void updatePrefix(Map<String, IrcUser> users, String key, char symbol, boolean adding) {
    IrcUser user = users.get(key);
    if (user == null) return;
    String value = user.prefixes.replace(String.valueOf(symbol), "");
    if (adding) value += symbol;
    String ordered = "";
    for (char prefix : prefixSymbols.toCharArray()) if (value.indexOf(prefix) >= 0) ordered += prefix;
    users.put(key, user.withPrefixes(ordered));
  }

  private static void memberAdd(Channel c, IrcUser u) {
    c.members.put(IrcParser.fold(u.nickname), u);
    if (c.namesInProgress) {
      c.namesRemoved.remove(IrcParser.fold(u.nickname));
      c.namesBatch.put(IrcParser.fold(u.nickname), u);
    }
  }

  private static void memberRemove(Channel c, String key) {
    c.members.remove(key);
    c.namesBatch.remove(key);
    if (c.namesInProgress) c.namesRemoved.add(key);
  }

  private static void event(Channel c, String from, String extra, String code) {
    c.add(
        new ChatMessage(
            from, extra, false, ChatMessage.Status.RECEIVED, code, System.currentTimeMillis()));
  }
}
