package com.example.fixclient1.fix;

import com.example.fixclient1.model.TableOrder;
import quickfix.Initiator;
import quickfix.Session;
import quickfix.SessionNotFound;
import quickfix.field.*;
import quickfix.fix44.NewOrderSingle;

public class SendFixMessage {
    public static void send(Initiator initiator, TableOrder tableOrder) throws SessionNotFound {

        char side = tableOrder.getSide().equalsIgnoreCase("BUY") ? Side.BUY : Side.SELL;
        char orderType = tableOrder.getOrderType().equalsIgnoreCase("LIMIT") ? OrdType.LIMIT : OrdType.MARKET;

        NewOrderSingle newOrder = new NewOrderSingle(
                new ClOrdID("123"),
                new Side(side),
                new TransactTime(),
                new OrdType(orderType)
        );

        newOrder.set(new Symbol(tableOrder.getSymbol()));
        newOrder.set(new OrderQty(tableOrder.getOrderQuantity()));
        newOrder.set(new Price(tableOrder.getOrderPrice()));

        Session.sendToTarget(newOrder, initiator.getSessions().get(0));
    }
}
