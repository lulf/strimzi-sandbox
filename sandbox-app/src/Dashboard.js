/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
import React, { Component } from 'react';
import Keycloak from 'keycloak-js';
import './App.css';
import { NavLink } from 'react-router-dom';
import TermsOfService from './TermsOfService.js';
import Features from './Features.js';
import { API_URL } from './constants';

var generateKubeConfig = function(kc, user, tenantNamespace) {
    return {
        apiVersion: "v1",
        kind: "Config",
        clusters: [
            {
                "name": "strimzi-sandbox",
                "cluster": {
                    "server": "https://kube-api.strimzi-sandbox.enmasse.io"
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
                            "idp-issuer-url": "https://auth.strimzi-sandbox.enmasse.io/auth/realms/k8s"
                        }
                    }
                }
            }
        ],
        "contexts": [
            {
                "name": "strimzi-sandbox",
                "context": {
                    "cluster": "strimzi-sandbox",
                    "namespace": tenantNamespace,
                    "user": user
                }
            }
        ],
        "current-context": "strimzi-sandbox"
    };
};


class Dashboard extends Component {
    constructor(props) {
        super(props);
        this.state = { keycloak: null, authenticated: false };
    }

    componentDidMount() {
        this.updateState();
    }

    componentWillUnmount() {
        clearInterval(this.timerId);
    }

    updateState() {
        const keycloak = new Keycloak('/keycloak.json');
        keycloak.init({onLoad: 'check-sso'}).then(authenticated => {
            var self = this;
            var state = { keycloak: keycloak, authenticated: authenticated };
            if (authenticated) {
                self.fetchData(keycloak, state);
                self.timerId = setInterval(function() {
                    self.fetchData(keycloak, state)
                }, 10000);
            } else {
                this.setState(state);
            }
        });
    }

    fetchData(keycloak, state) {
        var self = this;
        var token = keycloak.token;
        keycloak.loadUserProfile().then(function (profile) {
            fetch(API_URL + '/api/tenants/' + profile.username, {
                crossDomain: true,
                method: 'GET',
                headers: {
                    'Accept': 'application/json',
                    'Content-Type': 'application/json',
                    'Authorization': "Bearer " + token,
                },
            }).then((response) => {
                if (response.status < 200 || response.status >= 300) {
                    state.tenant = undefined;
                    self.setState(state);
                }
                return response.json();
            }).then((data) => {
                if (JSON.stringify(data) !== JSON.stringify(self.state.tenant)) {
                    state.tenant = data;
                    self.setState(state);
                }
            }).catch(function () {
                self.setState(state);
            });
        }).catch(function() {
            self.setState(state);
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
                        var expireDate = new Date(expirationTimestamp);
                        var expireDateStr = expireDate.toLocaleString();
                        var expireDays = Math.floor(timeUntilDeletion / (3600 * 24));
                        var expireHours = Math.floor(timeUntilDeletion % (3600 * 24) / 3600);
                        var kubeconfig = generateKubeConfig(this.state.keycloak, this.state.tenant.subject, this.state.tenant.namespace);
                        var bootstrapHostname = this.state.tenant.bootstrap;
                        var brokerHostnames = JSON.stringify(this.state.tenant.brokers);
                        var topicPrefix = this.state.tenant.namespace + ".";
                        return (
                            <div className="App">
                                <h3>Status</h3>
                                <input id="download" type="hidden" value={JSON.stringify(kubeconfig)} />
                                <table>
                                <tbody>
                                <tr><td>Logged in as</td><td>{this.state.tenant.subject}</td></tr>
                                <tr><td>Registered at</td><td>{creationDateStr}</td></tr>
                                <tr><td>Provisioned at</td><td>{provisionDateStr}</td></tr>
                                <tr><td>Expires at</td><td>{expireDateStr} (In {expireDays} days and {expireHours} hours)</td></tr>
                                <tr><td>Bootstrap Hostname</td><td>{bootstrapHostname}</td></tr>
                                <tr><td>Broker Hostname(s)</td><td>{brokerHostnames}</td></tr>
                                <tr><td>Topic prefix</td><td>{topicPrefix}</td></tr>
                                <tr><td>Kubeconfig</td><td><button onClick={this.downloadKubeconfig}>Download</button></td></tr>
                                </tbody>
                                </table>
                                <p>NOTE: Kafka clients need to prefix all topics with the above topic prefix.</p>
                                <p>For more information about how to use Strimzi, see the <a href="https://strimzi.io/documentation/">documentation</a>.</p>
                                <br />

                                <div>
                                <NavLink className="linkBlack" to="/unregister">Delete registration</NavLink>
                                </div>
                            </div>
                        );
                    } else {
                        var queueNum = this.state.tenant.placeInQueue;
                        if (queueNum !== undefined) {
                            var estimateProvisionTimestamp = Date.parse(this.state.tenant.estimatedProvisionTime);
                            var estimateProvisionDateStr = new Date(estimateProvisionTimestamp).toLocaleString();
                            return (
                                    <div className="App">
                                    <h3>Status</h3>
                                    <p>Environment not yet provisioned</p>
                                    <table>
                                    <tbody>
                                    <tr><td>Logged in as</td><td>{this.state.tenant.subject}</td></tr>
                                    <tr><td>Registered at</td><td>{creationDateStr}</td></tr>
                                    <tr><td>Place in provisioning queue</td><td>{queueNum}</td></tr>
                                    <tr><td>Estimated provisioning date</td><td>{estimateProvisionDateStr}</td></tr>
                                    </tbody>
                                    </table>
                                    <div className="InNavApp">
                                    <NavLink className="linkBlack" to="/unregister">Cancel registration</NavLink>
                                    </div>
                                    </div>
                            );
                        } else {
                            return (
                                    <div className="App">
                                    <h3>Status</h3>
                                    <p>Environment not yet provisioned</p>
                                    <table>
                                    <tbody>
                                    <tr><td>Logged in as</td><td>{this.state.tenant.subject}</td></tr>
                                    <tr><td>Registered at</td><td>{creationDateStr}</td></tr>
                                    </tbody>
                                    </table>
                                    <div className="InNavApp">
                                    <NavLink className="linkBlack" to="/unregister">Cancel</NavLink>
                                    </div>
                                    </div>
                            );
                        }
                    }
                } else {
                    return (
                            <div className="App">
                            <TermsOfService />
                            <Features />
                            <h3>Status</h3>
                            <p>Not registered</p>
                            <NavLink className="linkBlack" to="/register">Register</NavLink>
                            &nbsp;
                            &nbsp;
                            &nbsp;
                            &nbsp;
                            <NavLink className="linkBlack" to="/deleteuser">Forget me</NavLink>
                            </div>
                    );
                }
            } else {
                return (
                <div className="App">
                <TermsOfService />
                <Features />
                <NavLink className="linkBlack" to="/login">Login</NavLink>
                &nbsp;
                &nbsp;
                &nbsp;
                &nbsp;
                <NavLink className="linkBlack" to="/register">Register</NavLink>
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
        element.download = "strimzi-sandbox-kubeconfig.yaml";
        document.body.appendChild(element);
        element.click();
    };

    
};

export default Dashboard;
