import requests
from lxml import etree

def find_subelem(elem, name):
    ''' Given an etree graphic element, finds first subelement with given name '''
    for subelem in elem:
        if subelem.tag == name:
            return subelem
    return None

def find_subelem_list(elem, namelist):
    for name in namelist:
        elem = find_subelem(elem, name)
        if elem is None:
            print "find_subelem_list failed at element %s" % name
            return None
    return elem

server = "http://localhost:8080/reversi"
xml = '<?xml version="1.0" ?><request type="%s">%s</request>'
register_template = xml % ("register", "<nick>%s</nick>")
nick_uuid = '<nick>%s</nick><uuid>%s</uuid>'
join_template = xml % ("join", nick_uuid)
join_table_template = xml % ("join", nick_uuid + '<table>%s</table>')
move_template = xml % ("move", nick_uuid + '<position><row>%s</row><col>%s</col></position>')
pass_template = xml % ("pass", nick_uuid)
update_template = xml % ("update", nick_uuid + "<tableid>%s</tableid>")

class Player():
    def __init__(self, nick):
        self.nick = nick
        self.register()
        self.playing = False
        self.board = None
        self.color = None
        self.wins = None
        self.losses = None
        self.tableid = None

    def make_request(self, xml, method="POST"):
        if method == "POST":
            resp = requests.post(server, data=xml)
        elif method == "GET":
            resp = requests.get(server, data=xml)

        resp_xml = etree.fromstring(resp.content)
        return resp_xml

    def register(self):
        resp_xml = self.make_request(register_template % self.nick)
        uiElem = find_subelem(resp_xml, "userInfo")
        if uiElem is None:
            raise Exception("Didn't get a ui from the server")
        else:
            self.uuid = find_subelem(uiElem, "uuid").text

    def join(self, table=None):
        resp_xml = self.make_request(join_template % (self.nick, self.uuid))
        self.update(resp_xml)

    def move(self, row, col):
        print "%s is attempting to move to %d, %d" % (self.nick,row, col)
        resp_xml = self.make_request(move_template % (self.nick, 
                                                      self.uuid, 
                                                      row.__str__(), 
                                                      col.__str__()))
        self.update(resp_xml)

    def pass_turn(self):
        print "%s is attempting to pass turn" % self.nick
        resp_xml = self.make_request(pass_template % (self.nick, self.uuid))
        self.update(resp_xml)

    def update(self, resp = None):
        if resp is None:
            resp = self.make_request(update_template % (self.nick, 
                                                        self.uuid, 
                                                        self.tableid), 
                                                        "GET")

        if resp.get("type") == "error":
            print "Looks like we've got an error:"
            message = find_subelem(resp, "message")
            print message.text
            return

        tableInfo = find_subelem(resp, "tableInfo")
        if tableInfo is None:
            print "wtf mate"
            print etree.tostring(resp, pretty_print=True)
            raise Exception

        if not self.tableid:
            self.tableid = find_subelem(tableInfo, "tableid").text
        if not self.color:
            if find_subelem(tableInfo, "blackPlayer").text == self.nick:
                self.color = "black"
            else:
                self.color = "white"
        if find_subelem(tableInfo, "board") is not None:
            self.playing = True
            for row in find_subelem(tableInfo, "board"):
                print row.text
        userInfo = find_subelem(resp, "userInfo")
        if userInfo is not None:
            self.ratio = float(find_subelem(userInfo, "ratio").text)
            self.points = int(find_subelem(userInfo, "points").text)



foo = Player("foo")
foo.join()
foo.update()
bar = Player("bar")
bar.join()
foo.update()
foo.move(2, 3)
foo.move(2, 3)
bar.update()
bar.move(2, 2)
foo.update()
foo.move(3,2)
bar.update()
bar.move(2, 4)
foo.update()
foo.move(3, 5)
bar.move(4,2)
foo.update()
foo.pass_turn()
bar.move(3, 6)
foo.pass_turn()