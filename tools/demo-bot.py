import socket, time
NICK = 'Demo_Bot'
CHANNEL = '#web-general'
def run():
    with socket.create_connection(('127.0.0.1', 6667), timeout=10) as sock:
        sock.settimeout(None)
        def send(line):
            sock.sendall((line + '\r\n').encode('utf-8'))
        send('NICK ' + NICK)
        send('USER demo 0 * :Local demo bot')
        with sock.makefile('r', encoding='utf-8', errors='replace') as lines:
            for raw in lines:
                line = raw.rstrip('\r\n')
                if line.startswith('PING '):
                    send('PONG ' + line[5:])
                    continue
                trailing = ''
                if ' :' in line:
                    line, trailing = line.split(' :', 1)
                parts = line.split()
                if len(parts) < 2:
                    continue
                command = parts[1]
                sender = parts[0].lstrip(':').split('!')[0]
                if command == '001':
                    send('JOIN ' + CHANNEL)
                elif command == 'JOIN' and sender == NICK:
                    send('TOPIC ' + CHANNEL + ' :Demo USTH Connect tren may Mac - thu gui tin nhan va nhan rieng voi Demo_Bot')
                elif command == 'JOIN' and sender != NICK:
                    send('PRIVMSG ' + CHANNEL + ' :Chào ' + sender + '! Đây là bot demo cục bộ. Anh thử gửi tin nhắn hoặc mở Thành viên để nhắn riêng với Demo_Bot nhé.')
                elif command == 'PRIVMSG' and sender != NICK:
                    target = parts[2] if parts[2].startswith('#') else sender
                    send('PRIVMSG ' + target + ' :Demo đã nhận: ' + trailing[:120])
while True:
    try:
        run()
    except (OSError, UnicodeError):
        time.sleep(3)
