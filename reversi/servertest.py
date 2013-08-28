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
move_template = xml % ("move", '<position><row>%s</row><col>%s</col></position>')
update_template = xml % ("update", nick_uuid + "<tableid>%s</tableid>")

class Player():
    def __init__(self, nick):
        self.nick = nick
        self.register()
        self.playing = False

    def make_request(self, xml, method="POST"):
        if method == "POST":
            resp = requests.post(server, data=xml)
        elif method == "GET":
            resp = requests.get(server, data=xml)
        resp_xml = etree.fromstring(resp.content)
        return resp_xml

    def register(self):
        resp_xml = self.make_request(register_template % self.nick)
        uuidElem = find_subelem(resp_xml, "uuid")
        if uuidElem is None:
            raise Exception("Didn't get a uuid from the server")
        self.uuid = uuidElem.text

    def join(self, table=None):
        resp_xml = self.make_request(join_template % (self.nick, self.uuid))
        # print "Server responded:", etree.tostring(resp_xml, pretty_print=True)
        tableInfo = find_subelem(resp_xml, "tableInfo")
        self.tableid = find_subelem(tableInfo, "tableid").text
        if find_subelem(tableInfo, "blackPlayer").text == self.nick:
            self.color = "black"
        else:
            self.color = "white"
        if find_subelem(tableInfo, "board") is not None:
            print "Game started:"
            for row in find_subelem(tableInfo, "board"):
                print row.text

    def move(self, row, col):
        print "Attempting to move to %d, %d" % (row.__str__(), col.__str__())
        resp_xml = self.make_request(join_template % (row.__str__(), col.__str__()))
        print "Server responded:", etree.tostring(resp_xml, pretty_print=True)

    def update(self):
        resp = self.make_request(update_template % (self.nick, self.uuid, self.tableid, "GET")
        print "hi"




foo = Player("foo")
foo.join()
bar = Player("bar")
bar.join()