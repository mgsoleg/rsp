package com.rsp.protocol;


public class ServerInEvent extends BaseEvent {

    public static Builder getBuilder() {
        return new ServerInEvent().new Builder();
    }

    public class Builder {

        private Builder() {

        }

        public Builder appendEventTime(long eventTime) {
            ServerInEvent.this.eventTime = eventTime;
            return this;
        }

        public Builder appendChoice(byte choice) {
            ServerInEvent.this.choice = choice;
            return this;
        }

        public Builder appendUserName(char[] userName) {
            if (userName.length == 0 || userName.length > ServerInEvent.this.userName.length) {
                throw new IllegalStateException("Username length must be equals or less than " + ServerInEvent.this.userName.length);
            }
            System.arraycopy(userName, 0, ServerInEvent.this.userName, 0, userName.length);
            return this;
        }

        public ServerInEvent build() {
            return ServerInEvent.this;
        }

    }
}
