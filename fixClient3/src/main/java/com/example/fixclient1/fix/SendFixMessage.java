package com.example.fixclient1.fix;

import com.example.fixclient1.model.ReceivedData;
import com.example.fixclient1.model.TableOrder;
import quickfix.Initiator;
import quickfix.Session;
import quickfix.SessionNotFound;
import quickfix.field.*;
import quickfix.fix44.NewOrderSingle;
import quickfix.fix44.OrderCancelRequest;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

public class SendFixMessage {
    public static void send(Initiator initiator, TableOrder tableOrder) throws SessionNotFound {

        char side = tableOrder.getSide().equalsIgnoreCase("BUY") ? Side.BUY : Side.SELL;
        char orderType = tableOrder.getOrderType().equalsIgnoreCase("LIMIT") ? OrdType.LIMIT : OrdType.MARKET;

        NewOrderSingle newOrder = new NewOrderSingle(
                new ClOrdID("123" + System.currentTimeMillis()),
                new Side(side),
                new TransactTime(),
                new OrdType(orderType)
        );

        newOrder.set(new Symbol(tableOrder.getSymbol()));
        newOrder.set(new OrderQty(tableOrder.getOrderQuantity()));
        newOrder.set(new Price(tableOrder.getOrderPrice()));

        Session.sendToTarget(newOrder, initiator.getSessions().get(0));
    }

    public static void cancelOrder(Initiator initiator, ReceivedData receivedData) throws SessionNotFound {
        String cancelClOrdId = "CL" + System.currentTimeMillis();
        OrderCancelRequest cancelRequest = new OrderCancelRequest(
                new OrigClOrdID(receivedData.getClOrdId()),
                new ClOrdID(cancelClOrdId),
                new Side(receivedData.getSide().equals("BUY") ? Side.BUY : Side.SELL),
                new TransactTime(LocalDateTime.now())
        );
        cancelRequest.set(new Symbol(receivedData.getSymbol()));
        Session.sendToTarget(cancelRequest, initiator.getSessions().get(0));

    }
}
