import React, { Component } from 'react';
import Keycloak from 'keycloak-js';
import './App.css';
import { NavLink } from 'react-router-dom';

var generateKubeConfig = function(kc, user, tenantNamespace) {
    return {
        apiVersion: "v1",
        kind: "Config",
        clusters: [
            {
                "name": "enmasse-sandbox",
                "cluster": {
                    "server": "https://kube-api.sandbox.enmasse.io"
                }
            }
        ],
        users: [
            {
                "name": user,
                "user": {
                    "auth-provider": {
                        "name": "oidc",
                        "config": {
                            "client-id": "webapp",
                            "id-token": kc.idToken,
                            "refresh-token": kc.refreshToken,
                            "idp-issuer-url": "https://auth.sandbox.enmasse.io/auth/realms/k8s"
                        }
                    }
                }
            }
        ],
        "contexts": [
            {
                "name": "enmasse-sandbox",
                "context": {
                    "cluster": "enmasse-sandbox",
                    "namespace": tenantNamespace,
                    "user": user
                }
            }
        ],
        "current-context": "enmasse-sandbox"
    };
};


class Dashboard extends Component {
    constructor(props) {
        super(props);
        this.state = { keycloak: null, authenticated: false };
    }

    componentDidMount() {
        const keycloak = Keycloak('/keycloak.json');
        keycloak.init({onLoad: 'check-sso', promiseType: 'native'}).then(authenticated => {
            var self = this;
            var state = { keycloak: keycloak, authenticated: authenticated };
            if (authenticated) {
                var token = keycloak.token;
                keycloak.loadUserProfile().then(function (profile) {
                    fetch('https://api.sandbox.enmasse.io/api/tenants/' + profile.username, {
                        crossDomain: true,
                        method: 'GET',
                        headers: {
                            'Accept': 'application/json',
                            'Content-Type': 'application/json',
                            'Authorization': "Bearer " + token,
                        },
                    }).then((response) => {
                        return response.json();
                    }).then((data) => {
                        state.tenant = data;
                        self.setState(state);
                    }).catch(function () {
                        self.setState(state);
                    });
                }).catch(function() {
                    this.setState(state);
                });
            } else {
                this.setState(state);
            }
        });
    }

    render() {
        if (this.state.keycloak) {
            if (this.state.authenticated) {
                if (this.state.tenant !== undefined) {
                    var creationTimestamp = Date.parse(this.state.tenant.creationTimestamp);
                    var creationDate = new Date(creationTimestamp);
                    var creationDateStr = creationDate.toLocaleString();
                    if (this.state.tenant.provisionTimestamp !== undefined) {
                        var provisionTimestamp = Date.parse(this.state.tenant.provisionTimestamp);
                        var expirationTimestamp = Date.parse(this.state.tenant.expirationTimestamp);
                        var provisionDate = new Date(provisionTimestamp);
                        var provisionDateStr = provisionDate.toLocaleString();
                        var now = Date.now();
                        var timeUntilDeletion = (expirationTimestamp - now) / 1000;
                        var expireDays = Math.floor(timeUntilDeletion / (3600 * 24));
                        var expireHours = Math.floor(timeUntilDeletion % (3600 * 24) / 3600);
                        var kubeconfig = generateKubeConfig(this.state.keycloak, this.state.tenant.subject, this.state.tenant.namespace);
                        if (this.state.tenant.messagingUrl !== undefined) {
                            return (
                                <div className="App">
                                    <h3>Status</h3>
                                    <p>Logged in as {this.state.tenant.name}</p>
                                    <p>Registered at {creationDateStr}</p>
                                    <p>Provisioned at {provisionDateStr} (Expires in {expireDays} days and {expireHours} hours)</p>
                                    <p>Console: <a href="https://console.sandbox.enmasse.io">https://console.sandbox.enmasse.io</a></p>
                                    <p>Messaging: No address space created</p>
                                    <input id="download" type="hidden" value={kubeconfig} />
                                    <div className="InNavApp">
                                    <NavLink className="largeLinkBlack" to="/">{'<'} Back</NavLink>
                                    &nbsp;
                                    &nbsp;
                                    &nbsp;
                                    &nbsp;
                                    <NavLink className="largeLink" to="/Dashboard" onClick={this.downloadKubeconfig}>Download Kubeconfig</NavLink>
                                    </div>
                                </div>
                            );
                        } else {
                            var messagingUrl = this.state.tenant.messagingUrl;
                            return (
                                <div className="App">
                                    <h3>Status</h3>
                                    <p>Logged in as {this.state.tenant.name}</p>
                                    <p>Registered at {creationDateStr}</p>
                                    <p>Provisioned at {provisionDateStr} (Expires in {expireDays} days and {expireHours} hours)</p>
                                    <p>Console: <a href="https://console.sandbox.enmasse.io">https://console.sandbox.enmasse.io</a></p>
                                    <p>Messaging: {messagingUrl}</p>
                                    <input id="download" type="hidden" value={JSON.stringify(kubeconfig)} />
                                    <div className="InNavApp">
                                    <NavLink className="largeLinkBlack" to="/">{'<'} Back</NavLink>
                                    &nbsp;
                                    &nbsp;
                                    &nbsp;
                                    &nbsp;
                                    <NavLink className="largeLink" to="/Dashboard" onClick={this.downloadKubeconfig}>Download Kubeconfig</NavLink>
                                    </div>
                                </div>
                            );
                        }
                        
                    } else {
                        return (
                                <div className="App">
                                <h3>Status</h3>
                                <p>Logged in as {this.state.tenant.name}</p>
                                <p>Registered at {creationDateStr}</p>
                                <p>Environment not yet provisioned</p>
                                <div className="InNavApp">
                                <NavLink className="largeLinkBlack" to="/">{'<'} Back</NavLink>
                                </div>
                                </div>
                        );
                    }
                } else {
                    return (
                            <div className="App">
                            <h3>Not registered</h3>
                            <div className="InNavApp">
                            <NavLink className="largeLinkBlack" to="/">{'<'} Back</NavLink>
                            </div>
                            </div>
                    );
                }
            } else {
                return (
                        <div className="App">
                        <h3>Account not registered</h3>
                        <div className="InNavApp">
                        <NavLink className="largeLinkBlack" to="/">{'<'} Back</NavLink>
                        </div>
                        </div>
                );
            }
        }
        return (
                <div className="App">
                <h3>Loading...</h3>
                </div>
        );
    }

    downloadKubeconfig = () => {
        const element = document.createElement("a");
        const file = new Blob([document.getElementById('download').value], {type: "application/json"});
        element.href = URL.createObjectURL(file);
        element.download = "enmasse-sandbox-kubeconfig.yaml";
        document.body.appendChild(element);
        element.click();
    };

    
};

export default Dashboard;
