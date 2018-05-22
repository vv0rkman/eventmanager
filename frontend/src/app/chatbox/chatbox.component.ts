import {Component, Input, OnInit} from '@angular/core';
import {MessageService} from "../_services/message.service";
import {Message} from "../_models/message";
import {MessageDTOModel} from "../_models/dto/messageDTOModel";
import Stomp from 'stompjs';
import SockJS from 'sockjs.min.js';
import {UserService} from "../_services/user.service";

@Component({
  selector: 'chat-feature',
  templateUrl: './chatbox.component.html',
  styleUrls: ['./chatbox.component.css']
})
export class ChatboxComponent implements OnInit {
  @Input('eventId') eventId: string;

  messages: Message[];
  addingMessage = "";
  stompClient;

  constructor(private messageService: MessageService,
              private userService: UserService) {
  }

  ngOnInit() {
    this.getMessages(this.eventId);
    this.initializeWebSocketConnection();
  }

  initializeWebSocketConnection() {
    let ws = new SockJS("http://localhost:8090/socket");
    this.stompClient = Stomp.over(ws);
    let that = this;

    this.stompClient.connect({}, function (frame) {
      that.stompClient.subscribe("/chat", (message) => {
        let m: Message = JSON.parse(message.body);
        that.messages.push(m);
      });
    });
  }

  getMessages(chatId) {
    this.messageService.getAllByChatId(chatId)
      .subscribe((messages) => {
        this.messages = messages;
      })
  }

  addMessage(message: string) {

    let customerId = this.userService.getCurrentId();
    let m: MessageDTOModel = new MessageDTOModel();
    m.message = {
      id: '0',
      content: message,
      authorId: customerId,
      chatId: this.eventId,
      date: '2018-05-15 10:10:10'
    };
    console.log('look at me'+' '+ this.addingMessage);
    let that = this;

    that.stompClient.send('/api/messages', {}, JSON.stringify(m));

  }
}
