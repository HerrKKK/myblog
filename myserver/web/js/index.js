$(function() {
    $("#logOut").click(function() {
        $.ajax({
            type: 'POST',
            url: 'usermanage',
            data: {action: "logout"},
            // dataType: 'JSON',
            async: 'true',
            error : function() {
                console.log('failed to logout');
            },
            success: function() {
                sessionStorage.removeItem("token");
                location.reload();
            }
        });
    });
});