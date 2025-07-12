import os
import queue
import threading

import stomper
import websocket

from client_message import MSG


class WSClient:
    def __init__(self, client_id='c1', queue_size=1000, logger=None):
        if logger is None:
            raise RuntimeError('Need a logger.')
        self.q = queue.Queue(maxsize=queue_size)
        self.log = logger
        self.ws = None
        self.client_id = client_id
        self.init = False
        self.subscribed_ids = []
        self.bg_thread = threading.Thread(daemon=False, target=self.background_thread_runner, args=())

    def start(self):
        if self.init:
            raise RuntimeError('Should only start once.')
        self.init_ws()
        self.subscribe_all()
        self.bg_thread.start()

    # Call at the end to free up resources
    def cleanup(self):
        if not self.init:
            raise RuntimeError('You should start() the WSClient first.')
        self.bg_thread.join(timeout=1)  # Seconds
        self.unsubscribe()
        self.ws.send(stomper.disconnect())

    # BLOCK and get a message from the socket
    def drain_msg_queue(self):
        if not self.init:
            raise RuntimeError('You should start() the WSClient first.')
        result = []
        while True:
            try:
                result.append(self.q.get_nowait())
            except queue.Empty:
                break
        return result

    # BG runner
    def background_thread_runner(self):
        if not self.init:
            raise RuntimeError('You should start() the WSClient first.')
        while True:
            new_msg = MSG(self.ws.recv())
            self.q.put_nowait(new_msg)

    # Use the return values to unsubscribe later
    def subscribe_all(self):
        if not self.init:
            raise RuntimeError('You should start() the WSClient first.')
        self.subscribed_ids = [
            self.ws.send(stomper.subscribe('/topic/' + os.getenv('BENCHMARKING_STOMP_TOPIC'), self.client_id, ack='auto')),
            self.ws.send(stomper.subscribe('/topic/' + os.getenv('BO_STOMP_TOPIC'), self.client_id, ack='auto')),
            self.ws.send(stomper.subscribe('/topic/' + os.getenv('BROADCAST_STOMP_TOPIC'), self.client_id, ack='auto')),
        ]

    # Stop receiving messages from certain topics/queues
    def unsubscribe(self):
        if not self.init:
            raise RuntimeError('You should start() the WSClient first.')
        for id in self.subscribed_ids:
            self.ws.send(stomper.unsubscribe(id))

    # Send message to the Benchmarking topic
    def send_to_benchmark_topic(self, msg):
        if not self.init:
            raise RuntimeError('You should start() the WSClient first.')
        self.log.debug('Sending to benchmark topic: ' + msg)
        return self.ws.send(stomper.send('/app/' + os.getenv('BENCHMARKING_STOMP_TOPIC'), msg))

    # Send message to the BO topic
    def send_to_bo_topic(self, msg):
        if not self.init:
            raise RuntimeError('You should start() the WSClient first.')
        self.log.debug('Sending to BO topic: ' + msg)
        return self.ws.send(stomper.send('/app/' + os.getenv('BO_STOMP_TOPIC'), msg))

    # Send message to the BO topic
    def send_to_fs_topic(self, msg):
        if not self.init:
            raise RuntimeError('You should start() the WSClient first.')
        self.log.debug('Sending to BO topic: ' + msg)
        return self.ws.send(stomper.send('/app/' + os.getenv('FILE_SERVER_TOPIC'), msg))

    # Broadcast a notification in the corresponding topic
    def broadcast_notification(self, msg):
        if not self.init:
            raise RuntimeError('You should start() the WSClient first.')
        self.log.debug('Broadcasting ' + msg)
        return self.ws.send(stomper.send('/app/' + os.getenv('BROADCAST_STOMP_TOPIC'), msg))

    # Constructor
    def init_ws(self):
        if self.init:
            raise RuntimeError('You should start() the WSClient first.')
        self.ws = websocket.create_connection("ws://{0}/optimizer".format(os.getenv('OPTIMIZER_SERVER_URL')))
        self.ws.send(stomper.connect(
            username=os.getenv('STOMP_USERNAME'),
            password=os.getenv('STOMP_PASSWORD'),
            host="ws://{0}/optimizer".format(os.getenv('OPTIMIZER_SERVER_URL'))
        ))
        self.init = True
