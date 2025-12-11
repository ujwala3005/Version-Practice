def render() {
  writer.write("""
  <style>
.rw_login_theme {
    background: none !important;
}

body.rw_login_theme {
    background: none !important;
}
</style>
<script>
(function waitForButton() {
    var btn = document.getElementById("rw_logout_user_button");
    if (btn) {
        btn.addEventListener("click", function() {
            btn.href = "https://my-uat.cotiviti.com/login.jsp";
        });
    } else {
        setTimeout(waitForButton, 500);
    }
})();
</script>
""")
}
render()
