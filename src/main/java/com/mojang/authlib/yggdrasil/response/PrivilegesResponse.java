package com.mojang.authlib.yggdrasil.response;

public class PrivilegesResponse extends Response {
    private Privileges privileges = new Privileges();

    public Privileges getPrivileges() {
        return this.privileges;
    }

    public class Privileges {
        private Privilege onlineChat = new Privilege();

        private Privilege multiplayerServer = new Privilege();

        private Privilege multiplayerRealms = new Privilege();

        public Privilege getOnlineChat() {
            return this.onlineChat;
        }

        public Privilege getMultiplayerServer() {
            return this.multiplayerServer;
        }

        public Privilege getMultiplayerRealms() {
            return this.multiplayerRealms;
        }

        public class Privilege {
            private boolean enabled;

            public boolean isEnabled() {
                return this.enabled;
            }
        }
    }
}
