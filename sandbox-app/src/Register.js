import React, { Component } from 'react';
import Keycloak from 'keycloak-js';
import './App.css';

class Register extends Component {
  constructor(props) {
    super(props);
    this.state = { keycloak: null, authenticated: false };
  }

  componentDidMount() {
    const keycloak = Keycloak('/keycloak.json');
    keycloak.init({onLoad: 'login-required', promiseType: 'native'}).then(authenticated => {
      this.setState({ keycloak: keycloak, authenticated: authenticated })
    })
  }

  render() {
    if (this.state.keycloak) {
      if (this.state.authenticated) {
        fetch('http://localhost:8080/api/tenants', {
                crossDomain: true,
                method: 'POST',
                headers: {
                        'Accept': 'application/json',
                        'Content-Type': 'application/json',
                        'Authorization': "Bearer " + this.state.keycloak.:wq

                      },
                body: JSON.stringify({
                    name: "myuser"
                })
          }).then((response) => console.log(response));
        return (
        <div className="App">
          <h3>Logged in</h3>
        </div>
      );
      } else {
        return (
          <div className="App">
            <h3>Not logged in</h3>
          </div>
        )
      }
    }
    return (
      <div>Initializing Keycloak...</div>
    );
  }
};

export default Register;
