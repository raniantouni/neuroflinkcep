class MSG(object):
    def __init__(self, msg):
        if 'MESSAGE' in msg:
            self.msg = msg
            sp = self.msg.split("\n")
            self.destination = sp[1].split(":")[1]
            self.content = sp[2].split(":")[1]
            self.subs = sp[3].split(":")[1]
            self.id = sp[4].split(":")[1]
            self.len = sp[5].split(":")[1]
            self.message = ''.join(sp[7:])[0:-1]  # Actual payload
        else:
            self.msg = msg
            self.message = msg

    def is_from_id(self, client_id):
        return client_id in self.subs

    def __repr__(self):
        return self.message
