import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {AlertService} from "../../_services/alert.service";
import {WishListService} from "../../_services/wishlist.service";
import {Item} from "../../_models/wishList/item";
import {UserService} from "../../_services/user.service";
import {WishList} from "../../_models/wishList/wishList";
import {WishListItem} from "../../_models/wishList/wishListItem";

@Component({
  selector: 'app-all-items',
  templateUrl: './all-items.component.html',
  styleUrls: ['../wishlist/wishlist.component.css']
})
export class AllItemsComponent implements OnInit {
  @Input('included') isIncluded: boolean = false;
  @Output('itemView') outItemView = new EventEmitter<Item>();
  @Output('editableItem') outEditableItem = new EventEmitter<Item>();
  @Output('copiedItem') outCopiedItem = new EventEmitter<Item>();

  hasChanges: boolean = false;
  itemView: Item;
  editableItem: Item;
  copiedItem: Item;
  wishList: WishList;
  item: Item;
  items: Item[];
  trash: Item[];
  path: string[] = ['name'];
  order: number = 1;
  currentLogin: string;
  queryString: string;

  constructor(private wishListService: WishListService,
              private userService: UserService,
              private alertService: AlertService) {
    this.items = [];
    this.trash = [];
    this.item = new Item();
    this.item.tags = [];
    this.itemView = new Item();
    this.editableItem = new Item();
    this.copiedItem = new Item();
    this.queryString = '';
  }

  ngOnInit() {
    if (this.isIncluded)
      this.wishListService.wishList$.subscribe((wishList) => {
        this.wishList = wishList;
      });

    this.currentLogin = this.userService.getCurrentLogin();

    this.getAllItems();
  }

  copyItem(item: Item){
    this.isIncluded ?
      this.outCopiedItem.emit(item) :
      this.copiedItem = item;
  }

  isCreator(item: Item): boolean {
    return item.creator_customer_login == this.currentLogin;
  }

  editItem(item: Item): void {
    this.isIncluded ?
      this.outEditableItem.emit(item) :
      this.editableItem = item;
  }

  updateEditedItem(item: Item): void {
    let index = this.items.indexOf(this.editableItem);
    this.items.fill(item, index, index + 1);
  }


  showItemDetails(item: Item): void {
    this.itemView = item;
  }

  addCreatedItem(item: Item): void {
    this.items.push(item);
  }

  addItem(item: Item): void {
    let wishListItem: WishListItem = new WishListItem();

    // if (item.creator_customer_login != this.currentLogin) {
    //   item.creator_customer_login = this.currentLogin;
    //   this.addToCollection(item);
    // }

    wishListItem.item = item;

    wishListItem.event_id = this.wishList.id;
    wishListItem.priority = 3;
    this.wishList.items.push(wishListItem);
  }
  //
  // deleteItem(item: Item): void {
  //   let index = this.items.indexOf(item);
  //   this.items.splice(index, 1);
  //   let trash: Item[] = [];
  //   trash.push(item);
  //   this.wishListService.deleteItems(trash).subscribe(() =>
  //       this.alertService.success('Item successfully deleted!'),
  //     () => this.alertService.error('Something wrong'));
  // }


  deleteItem(item: Item): void {
    let index = this.items.indexOf(item);
    this.items.splice(index, 1);
    this.trash.push(item);
    this.hasChanges = true;
  }

  executeDelete(): void {
    if (this.trash.length > 0) {
      this.wishListService.deleteItems(this.trash).subscribe(() =>
          this.alertService.success('Items successfully deleted!'),
        () => this.alertService.error('Something wrong'));
    }
    this.hasChanges = false;
  }

  addToCollection(item: Item): void {
    this.item = item;
    delete this.item.id;
    this.item.creator_customer_login = this.currentLogin;

    this.wishListService.createItem(this.item)
      .subscribe(() => {
          this.alertService.success('Item added to you Collection.');
          this.items.push(this.item);
        },
        () => {
          this.alertService.error("Something wrong");
        });
  }

  getAllItems(): void {
    this.wishListService.getAllItems()
      .subscribe((items) => {
        this.items = items;
      }, () => {
        this.alertService.info('Items not found');
      });
  }

  sortItems(prop: string) {
    this.path = prop.split('.');
    this.order = this.order * (-1);
    return false;
  }
}
